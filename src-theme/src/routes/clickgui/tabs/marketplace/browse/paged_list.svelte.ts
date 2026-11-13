import {MarketplaceError} from "../../../../../integration/rest";
import type {MarketplacePage} from "../../../../../integration/types";

const UNREACHABLE_STATUS = 503;

/**
 * The pages of a marketplace list loaded so far. A reload drops the answer of every request before it.
 */
export class PagedList<T, P extends MarketplacePage<T> = MarketplacePage<T>> {

    items = $state<T[]>([]);
    lastPage = $state<P | null>(null);
    loading = $state(true);
    error = $state<string | null>(null);
    offline = $state(false);

    #request = 0;
    readonly #fetchPage: (page: number) => Promise<P>;

    constructor(fetchPage: (page: number) => Promise<P>) {
        this.#fetchPage = fetchPage;
    }

    async reload() {
        const request = ++this.#request;
        this.loading = true;
        try {
            const page = await this.#fetchPage(1);
            if (request !== this.#request) {
                return;
            }

            this.items = page.items;
            this.lastPage = page;
            this.error = null;
            this.offline = false;
        } catch (e) {
            if (request !== this.#request) {
                return;
            }

            this.items = [];
            this.lastPage = null;
            this.error = e instanceof Error ? e.message : String(e);
            this.offline = e instanceof MarketplaceError && e.status === UNREACHABLE_STATUS;
        }
        this.loading = false;
    }

    async loadMore() {
        const pagination = this.lastPage?.pagination;
        if (this.loading || !pagination || pagination.current >= pagination.pages) {
            return;
        }

        const request = this.#request;
        this.loading = true;
        const page = await this.#fetchPage(pagination.current + 1).catch(() => null);
        if (request !== this.#request) {
            return;
        }

        this.loading = false;
        if (page) {
            this.items = [...this.items, ...page.items];
            this.lastPage = page;
        }
    }

}
