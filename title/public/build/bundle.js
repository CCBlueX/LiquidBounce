
(function(l, r) { if (l.getElementById('livereloadscript')) return; r = l.createElement('script'); r.async = 1; r.src = '//' + (window.location.host || 'localhost').split(':')[0] + ':35907/livereload.js?snipver=1'; r.id = 'livereloadscript'; l.getElementsByTagName('head')[0].appendChild(r) })(window.document);
var app = (function () {
    'use strict';

    function noop() { }
    const identity = x => x;
    function assign(tar, src) {
        // @ts-ignore
        for (const k in src)
            tar[k] = src[k];
        return tar;
    }
    function add_location(element, file, line, column, char) {
        element.__svelte_meta = {
            loc: { file, line, column, char }
        };
    }
    function run(fn) {
        return fn();
    }
    function blank_object() {
        return Object.create(null);
    }
    function run_all(fns) {
        fns.forEach(run);
    }
    function is_function(thing) {
        return typeof thing === 'function';
    }
    function safe_not_equal(a, b) {
        return a != a ? b == b : a !== b || ((a && typeof a === 'object') || typeof a === 'function');
    }
    function is_empty(obj) {
        return Object.keys(obj).length === 0;
    }
    function create_slot(definition, ctx, $$scope, fn) {
        if (definition) {
            const slot_ctx = get_slot_context(definition, ctx, $$scope, fn);
            return definition[0](slot_ctx);
        }
    }
    function get_slot_context(definition, ctx, $$scope, fn) {
        return definition[1] && fn
            ? assign($$scope.ctx.slice(), definition[1](fn(ctx)))
            : $$scope.ctx;
    }
    function get_slot_changes(definition, $$scope, dirty, fn) {
        if (definition[2] && fn) {
            const lets = definition[2](fn(dirty));
            if ($$scope.dirty === undefined) {
                return lets;
            }
            if (typeof lets === 'object') {
                const merged = [];
                const len = Math.max($$scope.dirty.length, lets.length);
                for (let i = 0; i < len; i += 1) {
                    merged[i] = $$scope.dirty[i] | lets[i];
                }
                return merged;
            }
            return $$scope.dirty | lets;
        }
        return $$scope.dirty;
    }
    function update_slot(slot, slot_definition, ctx, $$scope, dirty, get_slot_changes_fn, get_slot_context_fn) {
        const slot_changes = get_slot_changes(slot_definition, $$scope, dirty, get_slot_changes_fn);
        if (slot_changes) {
            const slot_context = get_slot_context(slot_definition, ctx, $$scope, get_slot_context_fn);
            slot.p(slot_context, slot_changes);
        }
    }

    const is_client = typeof window !== 'undefined';
    let now = is_client
        ? () => window.performance.now()
        : () => Date.now();
    let raf = is_client ? cb => requestAnimationFrame(cb) : noop;

    const tasks = new Set();
    function run_tasks(now) {
        tasks.forEach(task => {
            if (!task.c(now)) {
                tasks.delete(task);
                task.f();
            }
        });
        if (tasks.size !== 0)
            raf(run_tasks);
    }
    /**
     * Creates a new task that runs on each raf frame
     * until it returns a falsy value or is aborted
     */
    function loop(callback) {
        let task;
        if (tasks.size === 0)
            raf(run_tasks);
        return {
            promise: new Promise(fulfill => {
                tasks.add(task = { c: callback, f: fulfill });
            }),
            abort() {
                tasks.delete(task);
            }
        };
    }

    function append(target, node) {
        target.appendChild(node);
    }
    function insert(target, node, anchor) {
        target.insertBefore(node, anchor || null);
    }
    function detach(node) {
        node.parentNode.removeChild(node);
    }
    function element(name) {
        return document.createElement(name);
    }
    function text(data) {
        return document.createTextNode(data);
    }
    function space() {
        return text(' ');
    }
    function listen(node, event, handler, options) {
        node.addEventListener(event, handler, options);
        return () => node.removeEventListener(event, handler, options);
    }
    function attr(node, attribute, value) {
        if (value == null)
            node.removeAttribute(attribute);
        else if (node.getAttribute(attribute) !== value)
            node.setAttribute(attribute, value);
    }
    function children(element) {
        return Array.from(element.childNodes);
    }
    function toggle_class(element, name, toggle) {
        element.classList[toggle ? 'add' : 'remove'](name);
    }
    function custom_event(type, detail) {
        const e = document.createEvent('CustomEvent');
        e.initCustomEvent(type, false, false, detail);
        return e;
    }

    const active_docs = new Set();
    let active = 0;
    // https://github.com/darkskyapp/string-hash/blob/master/index.js
    function hash(str) {
        let hash = 5381;
        let i = str.length;
        while (i--)
            hash = ((hash << 5) - hash) ^ str.charCodeAt(i);
        return hash >>> 0;
    }
    function create_rule(node, a, b, duration, delay, ease, fn, uid = 0) {
        const step = 16.666 / duration;
        let keyframes = '{\n';
        for (let p = 0; p <= 1; p += step) {
            const t = a + (b - a) * ease(p);
            keyframes += p * 100 + `%{${fn(t, 1 - t)}}\n`;
        }
        const rule = keyframes + `100% {${fn(b, 1 - b)}}\n}`;
        const name = `__svelte_${hash(rule)}_${uid}`;
        const doc = node.ownerDocument;
        active_docs.add(doc);
        const stylesheet = doc.__svelte_stylesheet || (doc.__svelte_stylesheet = doc.head.appendChild(element('style')).sheet);
        const current_rules = doc.__svelte_rules || (doc.__svelte_rules = {});
        if (!current_rules[name]) {
            current_rules[name] = true;
            stylesheet.insertRule(`@keyframes ${name} ${rule}`, stylesheet.cssRules.length);
        }
        const animation = node.style.animation || '';
        node.style.animation = `${animation ? `${animation}, ` : ''}${name} ${duration}ms linear ${delay}ms 1 both`;
        active += 1;
        return name;
    }
    function delete_rule(node, name) {
        const previous = (node.style.animation || '').split(', ');
        const next = previous.filter(name
            ? anim => anim.indexOf(name) < 0 // remove specific animation
            : anim => anim.indexOf('__svelte') === -1 // remove all Svelte animations
        );
        const deleted = previous.length - next.length;
        if (deleted) {
            node.style.animation = next.join(', ');
            active -= deleted;
            if (!active)
                clear_rules();
        }
    }
    function clear_rules() {
        raf(() => {
            if (active)
                return;
            active_docs.forEach(doc => {
                const stylesheet = doc.__svelte_stylesheet;
                let i = stylesheet.cssRules.length;
                while (i--)
                    stylesheet.deleteRule(i);
                doc.__svelte_rules = {};
            });
            active_docs.clear();
        });
    }

    let current_component;
    function set_current_component(component) {
        current_component = component;
    }
    function get_current_component() {
        if (!current_component)
            throw new Error('Function called outside component initialization');
        return current_component;
    }
    function afterUpdate(fn) {
        get_current_component().$$.after_update.push(fn);
    }
    function createEventDispatcher() {
        const component = get_current_component();
        return (type, detail) => {
            const callbacks = component.$$.callbacks[type];
            if (callbacks) {
                // TODO are there situations where events could be dispatched
                // in a server (non-DOM) environment?
                const event = custom_event(type, detail);
                callbacks.slice().forEach(fn => {
                    fn.call(component, event);
                });
            }
        };
    }

    const dirty_components = [];
    const binding_callbacks = [];
    const render_callbacks = [];
    const flush_callbacks = [];
    const resolved_promise = Promise.resolve();
    let update_scheduled = false;
    function schedule_update() {
        if (!update_scheduled) {
            update_scheduled = true;
            resolved_promise.then(flush);
        }
    }
    function add_render_callback(fn) {
        render_callbacks.push(fn);
    }
    let flushing = false;
    const seen_callbacks = new Set();
    function flush() {
        if (flushing)
            return;
        flushing = true;
        do {
            // first, call beforeUpdate functions
            // and update components
            for (let i = 0; i < dirty_components.length; i += 1) {
                const component = dirty_components[i];
                set_current_component(component);
                update(component.$$);
            }
            set_current_component(null);
            dirty_components.length = 0;
            while (binding_callbacks.length)
                binding_callbacks.pop()();
            // then, once components are updated, call
            // afterUpdate functions. This may cause
            // subsequent updates...
            for (let i = 0; i < render_callbacks.length; i += 1) {
                const callback = render_callbacks[i];
                if (!seen_callbacks.has(callback)) {
                    // ...so guard against infinite loops
                    seen_callbacks.add(callback);
                    callback();
                }
            }
            render_callbacks.length = 0;
        } while (dirty_components.length);
        while (flush_callbacks.length) {
            flush_callbacks.pop()();
        }
        update_scheduled = false;
        flushing = false;
        seen_callbacks.clear();
    }
    function update($$) {
        if ($$.fragment !== null) {
            $$.update();
            run_all($$.before_update);
            const dirty = $$.dirty;
            $$.dirty = [-1];
            $$.fragment && $$.fragment.p($$.ctx, dirty);
            $$.after_update.forEach(add_render_callback);
        }
    }

    let promise;
    function wait() {
        if (!promise) {
            promise = Promise.resolve();
            promise.then(() => {
                promise = null;
            });
        }
        return promise;
    }
    function dispatch(node, direction, kind) {
        node.dispatchEvent(custom_event(`${direction ? 'intro' : 'outro'}${kind}`));
    }
    const outroing = new Set();
    let outros;
    function group_outros() {
        outros = {
            r: 0,
            c: [],
            p: outros // parent group
        };
    }
    function check_outros() {
        if (!outros.r) {
            run_all(outros.c);
        }
        outros = outros.p;
    }
    function transition_in(block, local) {
        if (block && block.i) {
            outroing.delete(block);
            block.i(local);
        }
    }
    function transition_out(block, local, detach, callback) {
        if (block && block.o) {
            if (outroing.has(block))
                return;
            outroing.add(block);
            outros.c.push(() => {
                outroing.delete(block);
                if (callback) {
                    if (detach)
                        block.d(1);
                    callback();
                }
            });
            block.o(local);
        }
    }
    const null_transition = { duration: 0 };
    function create_bidirectional_transition(node, fn, params, intro) {
        let config = fn(node, params);
        let t = intro ? 0 : 1;
        let running_program = null;
        let pending_program = null;
        let animation_name = null;
        function clear_animation() {
            if (animation_name)
                delete_rule(node, animation_name);
        }
        function init(program, duration) {
            const d = program.b - t;
            duration *= Math.abs(d);
            return {
                a: t,
                b: program.b,
                d,
                duration,
                start: program.start,
                end: program.start + duration,
                group: program.group
            };
        }
        function go(b) {
            const { delay = 0, duration = 300, easing = identity, tick = noop, css } = config || null_transition;
            const program = {
                start: now() + delay,
                b
            };
            if (!b) {
                // @ts-ignore todo: improve typings
                program.group = outros;
                outros.r += 1;
            }
            if (running_program || pending_program) {
                pending_program = program;
            }
            else {
                // if this is an intro, and there's a delay, we need to do
                // an initial tick and/or apply CSS animation immediately
                if (css) {
                    clear_animation();
                    animation_name = create_rule(node, t, b, duration, delay, easing, css);
                }
                if (b)
                    tick(0, 1);
                running_program = init(program, duration);
                add_render_callback(() => dispatch(node, b, 'start'));
                loop(now => {
                    if (pending_program && now > pending_program.start) {
                        running_program = init(pending_program, duration);
                        pending_program = null;
                        dispatch(node, running_program.b, 'start');
                        if (css) {
                            clear_animation();
                            animation_name = create_rule(node, t, running_program.b, running_program.duration, 0, easing, config.css);
                        }
                    }
                    if (running_program) {
                        if (now >= running_program.end) {
                            tick(t = running_program.b, 1 - t);
                            dispatch(node, running_program.b, 'end');
                            if (!pending_program) {
                                // we're done
                                if (running_program.b) {
                                    // intro — we can tidy up immediately
                                    clear_animation();
                                }
                                else {
                                    // outro — needs to be coordinated
                                    if (!--running_program.group.r)
                                        run_all(running_program.group.c);
                                }
                            }
                            running_program = null;
                        }
                        else if (now >= running_program.start) {
                            const p = now - running_program.start;
                            t = running_program.a + running_program.d * easing(p / running_program.duration);
                            tick(t, 1 - t);
                        }
                    }
                    return !!(running_program || pending_program);
                });
            }
        }
        return {
            run(b) {
                if (is_function(config)) {
                    wait().then(() => {
                        // @ts-ignore
                        config = config();
                        go(b);
                    });
                }
                else {
                    go(b);
                }
            },
            end() {
                clear_animation();
                running_program = pending_program = null;
            }
        };
    }
    function create_component(block) {
        block && block.c();
    }
    function mount_component(component, target, anchor, customElement) {
        const { fragment, on_mount, on_destroy, after_update } = component.$$;
        fragment && fragment.m(target, anchor);
        if (!customElement) {
            // onMount happens before the initial afterUpdate
            add_render_callback(() => {
                const new_on_destroy = on_mount.map(run).filter(is_function);
                if (on_destroy) {
                    on_destroy.push(...new_on_destroy);
                }
                else {
                    // Edge case - component was destroyed immediately,
                    // most likely as a result of a binding initialising
                    run_all(new_on_destroy);
                }
                component.$$.on_mount = [];
            });
        }
        after_update.forEach(add_render_callback);
    }
    function destroy_component(component, detaching) {
        const $$ = component.$$;
        if ($$.fragment !== null) {
            run_all($$.on_destroy);
            $$.fragment && $$.fragment.d(detaching);
            // TODO null out other refs, including component.$$ (but need to
            // preserve final state?)
            $$.on_destroy = $$.fragment = null;
            $$.ctx = [];
        }
    }
    function make_dirty(component, i) {
        if (component.$$.dirty[0] === -1) {
            dirty_components.push(component);
            schedule_update();
            component.$$.dirty.fill(0);
        }
        component.$$.dirty[(i / 31) | 0] |= (1 << (i % 31));
    }
    function init(component, options, instance, create_fragment, not_equal, props, dirty = [-1]) {
        const parent_component = current_component;
        set_current_component(component);
        const $$ = component.$$ = {
            fragment: null,
            ctx: null,
            // state
            props,
            update: noop,
            not_equal,
            bound: blank_object(),
            // lifecycle
            on_mount: [],
            on_destroy: [],
            on_disconnect: [],
            before_update: [],
            after_update: [],
            context: new Map(parent_component ? parent_component.$$.context : options.context || []),
            // everything else
            callbacks: blank_object(),
            dirty,
            skip_bound: false
        };
        let ready = false;
        $$.ctx = instance
            ? instance(component, options.props || {}, (i, ret, ...rest) => {
                const value = rest.length ? rest[0] : ret;
                if ($$.ctx && not_equal($$.ctx[i], $$.ctx[i] = value)) {
                    if (!$$.skip_bound && $$.bound[i])
                        $$.bound[i](value);
                    if (ready)
                        make_dirty(component, i);
                }
                return ret;
            })
            : [];
        $$.update();
        ready = true;
        run_all($$.before_update);
        // `false` as a special case of no DOM component
        $$.fragment = create_fragment ? create_fragment($$.ctx) : false;
        if (options.target) {
            if (options.hydrate) {
                const nodes = children(options.target);
                // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
                $$.fragment && $$.fragment.l(nodes);
                nodes.forEach(detach);
            }
            else {
                // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
                $$.fragment && $$.fragment.c();
            }
            if (options.intro)
                transition_in(component.$$.fragment);
            mount_component(component, options.target, options.anchor, options.customElement);
            flush();
        }
        set_current_component(parent_component);
    }
    /**
     * Base class for Svelte components. Used when dev=false.
     */
    class SvelteComponent {
        $destroy() {
            destroy_component(this, 1);
            this.$destroy = noop;
        }
        $on(type, callback) {
            const callbacks = (this.$$.callbacks[type] || (this.$$.callbacks[type] = []));
            callbacks.push(callback);
            return () => {
                const index = callbacks.indexOf(callback);
                if (index !== -1)
                    callbacks.splice(index, 1);
            };
        }
        $set($$props) {
            if (this.$$set && !is_empty($$props)) {
                this.$$.skip_bound = true;
                this.$$set($$props);
                this.$$.skip_bound = false;
            }
        }
    }

    function dispatch_dev(type, detail) {
        document.dispatchEvent(custom_event(type, Object.assign({ version: '3.38.2' }, detail)));
    }
    function append_dev(target, node) {
        dispatch_dev('SvelteDOMInsert', { target, node });
        append(target, node);
    }
    function insert_dev(target, node, anchor) {
        dispatch_dev('SvelteDOMInsert', { target, node, anchor });
        insert(target, node, anchor);
    }
    function detach_dev(node) {
        dispatch_dev('SvelteDOMRemove', { node });
        detach(node);
    }
    function listen_dev(node, event, handler, options, has_prevent_default, has_stop_propagation) {
        const modifiers = options === true ? ['capture'] : options ? Array.from(Object.keys(options)) : [];
        if (has_prevent_default)
            modifiers.push('preventDefault');
        if (has_stop_propagation)
            modifiers.push('stopPropagation');
        dispatch_dev('SvelteDOMAddEventListener', { node, event, handler, modifiers });
        const dispose = listen(node, event, handler, options);
        return () => {
            dispatch_dev('SvelteDOMRemoveEventListener', { node, event, handler, modifiers });
            dispose();
        };
    }
    function attr_dev(node, attribute, value) {
        attr(node, attribute, value);
        if (value == null)
            dispatch_dev('SvelteDOMRemoveAttribute', { node, attribute });
        else
            dispatch_dev('SvelteDOMSetAttribute', { node, attribute, value });
    }
    function set_data_dev(text, data) {
        data = '' + data;
        if (text.wholeText === data)
            return;
        dispatch_dev('SvelteDOMSetData', { node: text, data });
        text.data = data;
    }
    function validate_slots(name, slot, keys) {
        for (const slot_key of Object.keys(slot)) {
            if (!~keys.indexOf(slot_key)) {
                console.warn(`<${name}> received an unexpected slot "${slot_key}".`);
            }
        }
    }
    /**
     * Base class for Svelte components with some minor dev-enhancements. Used when dev=true.
     */
    class SvelteComponentDev extends SvelteComponent {
        constructor(options) {
            if (!options || (!options.target && !options.$$inline)) {
                throw new Error("'target' is a required option");
            }
            super();
        }
        $destroy() {
            super.$destroy();
            this.$destroy = () => {
                console.warn('Component was already destroyed'); // eslint-disable-line no-console
            };
        }
        $capture_state() { }
        $inject_state() { }
    }

    function cubicOut(t) {
        const f = t - 1.0;
        return f * f * f + 1.0;
    }

    function fade(node, { delay = 0, duration = 400, easing = identity } = {}) {
        const o = +getComputedStyle(node).opacity;
        return {
            delay,
            duration,
            easing,
            css: t => `opacity: ${t * o}`
        };
    }
    function fly(node, { delay = 0, duration = 400, easing = cubicOut, x = 0, y = 0, opacity = 0 } = {}) {
        const style = getComputedStyle(node);
        const target_opacity = +style.opacity;
        const transform = style.transform === 'none' ? '' : style.transform;
        const od = target_opacity * (1 - opacity);
        return {
            delay,
            duration,
            easing,
            css: (t, u) => `
			transform: ${transform} translate(${(1 - t) * x}px, ${(1 - t) * y}px);
			opacity: ${target_opacity - (od * u)}`
        };
    }

    /* src\elements\ToolTip.svelte generated by Svelte v3.38.2 */
    const file$9 = "src\\elements\\ToolTip.svelte";

    // (22:4) {#if shown}
    function create_if_block$2(ctx) {
    	let div;
    	let t;
    	let div_transition;
    	let current;

    	const block = {
    		c: function create() {
    			div = element("div");
    			t = text(/*text*/ ctx[0]);
    			attr_dev(div, "class", "tooltip svelte-upwthj");
    			add_location(div, file$9, 22, 8, 489);
    		},
    		m: function mount(target, anchor) {
    			insert_dev(target, div, anchor);
    			append_dev(div, t);
    			current = true;
    		},
    		p: function update(ctx, dirty) {
    			if (!current || dirty & /*text*/ 1) set_data_dev(t, /*text*/ ctx[0]);
    		},
    		i: function intro(local) {
    			if (current) return;

    			add_render_callback(() => {
    				if (!div_transition) div_transition = create_bidirectional_transition(div, fly, { y: -10, duration: 200 }, true);
    				div_transition.run(1);
    			});

    			current = true;
    		},
    		o: function outro(local) {
    			if (!div_transition) div_transition = create_bidirectional_transition(div, fly, { y: -10, duration: 200 }, false);
    			div_transition.run(0);
    			current = false;
    		},
    		d: function destroy(detaching) {
    			if (detaching) detach_dev(div);
    			if (detaching && div_transition) div_transition.end();
    		}
    	};

    	dispatch_dev("SvelteRegisterBlock", {
    		block,
    		id: create_if_block$2.name,
    		type: "if",
    		source: "(22:4) {#if shown}",
    		ctx
    	});

    	return block;
    }

    function create_fragment$9(ctx) {
    	let div;
    	let current;
    	let if_block = /*shown*/ ctx[2] && create_if_block$2(ctx);

    	const block = {
    		c: function create() {
    			div = element("div");
    			if (if_block) if_block.c();
    			add_location(div, file$9, 20, 0, 437);
    		},
    		l: function claim(nodes) {
    			throw new Error("options.hydrate only works if the component was compiled with the `hydratable: true` option");
    		},
    		m: function mount(target, anchor) {
    			insert_dev(target, div, anchor);
    			if (if_block) if_block.m(div, null);
    			/*div_binding*/ ctx[3](div);
    			current = true;
    		},
    		p: function update(ctx, [dirty]) {
    			if (/*shown*/ ctx[2]) {
    				if (if_block) {
    					if_block.p(ctx, dirty);

    					if (dirty & /*shown*/ 4) {
    						transition_in(if_block, 1);
    					}
    				} else {
    					if_block = create_if_block$2(ctx);
    					if_block.c();
    					transition_in(if_block, 1);
    					if_block.m(div, null);
    				}
    			} else if (if_block) {
    				group_outros();

    				transition_out(if_block, 1, 1, () => {
    					if_block = null;
    				});

    				check_outros();
    			}
    		},
    		i: function intro(local) {
    			if (current) return;
    			transition_in(if_block);
    			current = true;
    		},
    		o: function outro(local) {
    			transition_out(if_block);
    			current = false;
    		},
    		d: function destroy(detaching) {
    			if (detaching) detach_dev(div);
    			if (if_block) if_block.d();
    			/*div_binding*/ ctx[3](null);
    		}
    	};

    	dispatch_dev("SvelteRegisterBlock", {
    		block,
    		id: create_fragment$9.name,
    		type: "component",
    		source: "",
    		ctx
    	});

    	return block;
    }

    function instance$9($$self, $$props, $$invalidate) {
    	let { $$slots: slots = {}, $$scope } = $$props;
    	validate_slots("ToolTip", slots, []);
    	let { text } = $$props;
    	let element;
    	let shown = false;

    	afterUpdate(() => {
    		element.parentNode.addEventListener("mouseenter", e => {
    			$$invalidate(2, shown = true);
    		});

    		element.parentNode.addEventListener("mouseleave", e => {
    			$$invalidate(2, shown = false);
    		});
    	});

    	const writable_props = ["text"];

    	Object.keys($$props).forEach(key => {
    		if (!~writable_props.indexOf(key) && key.slice(0, 2) !== "$$") console.warn(`<ToolTip> was created with unknown prop '${key}'`);
    	});

    	function div_binding($$value) {
    		binding_callbacks[$$value ? "unshift" : "push"](() => {
    			element = $$value;
    			$$invalidate(1, element);
    		});
    	}

    	$$self.$$set = $$props => {
    		if ("text" in $$props) $$invalidate(0, text = $$props.text);
    	};

    	$$self.$capture_state = () => ({
    		fade,
    		fly,
    		afterUpdate,
    		text,
    		element,
    		shown
    	});

    	$$self.$inject_state = $$props => {
    		if ("text" in $$props) $$invalidate(0, text = $$props.text);
    		if ("element" in $$props) $$invalidate(1, element = $$props.element);
    		if ("shown" in $$props) $$invalidate(2, shown = $$props.shown);
    	};

    	if ($$props && "$$inject" in $$props) {
    		$$self.$inject_state($$props.$$inject);
    	}

    	return [text, element, shown, div_binding];
    }

    class ToolTip extends SvelteComponentDev {
    	constructor(options) {
    		super(options);
    		init(this, options, instance$9, create_fragment$9, safe_not_equal, { text: 0 });

    		dispatch_dev("SvelteRegisterComponent", {
    			component: this,
    			tagName: "ToolTip",
    			options,
    			id: create_fragment$9.name
    		});

    		const { ctx } = this.$$;
    		const props = options.props || {};

    		if (/*text*/ ctx[0] === undefined && !("text" in props)) {
    			console.warn("<ToolTip> was created without expected prop 'text'");
    		}
    	}

    	get text() {
    		throw new Error("<ToolTip>: Props cannot be read directly from the component instance unless compiling with 'accessors: true' or '<svelte:options accessors/>'");
    	}

    	set text(value) {
    		throw new Error("<ToolTip>: Props cannot be set directly on the component instance unless compiling with 'accessors: true' or '<svelte:options accessors/>'");
    	}
    }

    /* src\elements\Account.svelte generated by Svelte v3.38.2 */
    const file$8 = "src\\elements\\Account.svelte";

    function create_fragment$8(ctx) {
    	let div5;
    	let div1;
    	let div0;
    	let tooltip;
    	let t0;
    	let img0;
    	let img0_src_value;
    	let t1;
    	let img1;
    	let img1_src_value;
    	let t2;
    	let div2;
    	let t3;
    	let t4;
    	let div3;
    	let t5;
    	let t6;
    	let div4;
    	let img2;
    	let img2_src_value;
    	let current;
    	let mounted;
    	let dispose;

    	tooltip = new ToolTip({
    			props: { text: "Change location" },
    			$$inline: true
    		});

    	const block = {
    		c: function create() {
    			div5 = element("div");
    			div1 = element("div");
    			div0 = element("div");
    			create_component(tooltip.$$.fragment);
    			t0 = space();
    			img0 = element("img");
    			t1 = space();
    			img1 = element("img");
    			t2 = space();
    			div2 = element("div");
    			t3 = text(/*username*/ ctx[0]);
    			t4 = space();
    			div3 = element("div");
    			t5 = text(/*lastUsed*/ ctx[1]);
    			t6 = space();
    			div4 = element("div");
    			img2 = element("img");
    			attr_dev(img0, "class", "location svelte-4tm950");
    			if (img0.src !== (img0_src_value = "img/flags/" + /*location*/ ctx[2] + ".svg")) attr_dev(img0, "src", img0_src_value);
    			attr_dev(img0, "alt", /*location*/ ctx[2]);
    			add_location(img0, file$8, 24, 12, 610);
    			attr_dev(div0, "class", "location-wrapper svelte-4tm950");
    			add_location(div0, file$8, 22, 8, 518);
    			attr_dev(img1, "class", "head svelte-4tm950");
    			if (img1.src !== (img1_src_value = "https://crafatar.com/avatars/3aa1c8378f4948e9ab4d7a7c36679748")) attr_dev(img1, "src", img1_src_value);
    			attr_dev(img1, "alt", "head");
    			add_location(img1, file$8, 26, 8, 735);
    			attr_dev(div1, "class", "icon svelte-4tm950");
    			add_location(div1, file$8, 21, 4, 490);
    			attr_dev(div2, "class", "username svelte-4tm950");
    			add_location(div2, file$8, 28, 4, 850);
    			attr_dev(div3, "class", "last-used svelte-4tm950");
    			add_location(div3, file$8, 29, 4, 894);
    			if (img2.src !== (img2_src_value = "img/icons/pen.svg")) attr_dev(img2, "src", img2_src_value);
    			attr_dev(img2, "alt", "pen");
    			add_location(img2, file$8, 31, 8, 977);
    			attr_dev(div4, "class", "change-account svelte-4tm950");
    			add_location(div4, file$8, 30, 4, 939);
    			attr_dev(div5, "class", "account svelte-4tm950");
    			add_location(div5, file$8, 20, 0, 433);
    		},
    		l: function claim(nodes) {
    			throw new Error("options.hydrate only works if the component was compiled with the `hydratable: true` option");
    		},
    		m: function mount(target, anchor) {
    			insert_dev(target, div5, anchor);
    			append_dev(div5, div1);
    			append_dev(div1, div0);
    			mount_component(tooltip, div0, null);
    			append_dev(div0, t0);
    			append_dev(div0, img0);
    			append_dev(div1, t1);
    			append_dev(div1, img1);
    			append_dev(div5, t2);
    			append_dev(div5, div2);
    			append_dev(div2, t3);
    			append_dev(div5, t4);
    			append_dev(div5, div3);
    			append_dev(div3, t5);
    			append_dev(div5, t6);
    			append_dev(div5, div4);
    			append_dev(div4, img2);
    			current = true;

    			if (!mounted) {
    				dispose = [
    					listen_dev(img0, "click", /*handleLocationClick*/ ctx[4], false, false, false),
    					listen_dev(div5, "click", /*handleAccountClick*/ ctx[3], false, false, false)
    				];

    				mounted = true;
    			}
    		},
    		p: function update(ctx, [dirty]) {
    			if (!current || dirty & /*location*/ 4 && img0.src !== (img0_src_value = "img/flags/" + /*location*/ ctx[2] + ".svg")) {
    				attr_dev(img0, "src", img0_src_value);
    			}

    			if (!current || dirty & /*location*/ 4) {
    				attr_dev(img0, "alt", /*location*/ ctx[2]);
    			}

    			if (!current || dirty & /*username*/ 1) set_data_dev(t3, /*username*/ ctx[0]);
    			if (!current || dirty & /*lastUsed*/ 2) set_data_dev(t5, /*lastUsed*/ ctx[1]);
    		},
    		i: function intro(local) {
    			if (current) return;
    			transition_in(tooltip.$$.fragment, local);
    			current = true;
    		},
    		o: function outro(local) {
    			transition_out(tooltip.$$.fragment, local);
    			current = false;
    		},
    		d: function destroy(detaching) {
    			if (detaching) detach_dev(div5);
    			destroy_component(tooltip);
    			mounted = false;
    			run_all(dispose);
    		}
    	};

    	dispatch_dev("SvelteRegisterBlock", {
    		block,
    		id: create_fragment$8.name,
    		type: "component",
    		source: "",
    		ctx
    	});

    	return block;
    }

    function instance$8($$self, $$props, $$invalidate) {
    	let { $$slots: slots = {}, $$scope } = $$props;
    	validate_slots("Account", slots, []);
    	const dispatch = createEventDispatcher();
    	let { username } = $$props;
    	let { lastUsed } = $$props;
    	let { location } = $$props;

    	function handleAccountClick(e) {
    		dispatch("altManagerClick");
    	}

    	function handleLocationClick(e) {
    		dispatch("altManagerClick");
    	}

    	const writable_props = ["username", "lastUsed", "location"];

    	Object.keys($$props).forEach(key => {
    		if (!~writable_props.indexOf(key) && key.slice(0, 2) !== "$$") console.warn(`<Account> was created with unknown prop '${key}'`);
    	});

    	$$self.$$set = $$props => {
    		if ("username" in $$props) $$invalidate(0, username = $$props.username);
    		if ("lastUsed" in $$props) $$invalidate(1, lastUsed = $$props.lastUsed);
    		if ("location" in $$props) $$invalidate(2, location = $$props.location);
    	};

    	$$self.$capture_state = () => ({
    		createEventDispatcher,
    		dispatch,
    		ToolTip,
    		username,
    		lastUsed,
    		location,
    		handleAccountClick,
    		handleLocationClick
    	});

    	$$self.$inject_state = $$props => {
    		if ("username" in $$props) $$invalidate(0, username = $$props.username);
    		if ("lastUsed" in $$props) $$invalidate(1, lastUsed = $$props.lastUsed);
    		if ("location" in $$props) $$invalidate(2, location = $$props.location);
    	};

    	if ($$props && "$$inject" in $$props) {
    		$$self.$inject_state($$props.$$inject);
    	}

    	return [username, lastUsed, location, handleAccountClick, handleLocationClick];
    }

    class Account extends SvelteComponentDev {
    	constructor(options) {
    		super(options);
    		init(this, options, instance$8, create_fragment$8, safe_not_equal, { username: 0, lastUsed: 1, location: 2 });

    		dispatch_dev("SvelteRegisterComponent", {
    			component: this,
    			tagName: "Account",
    			options,
    			id: create_fragment$8.name
    		});

    		const { ctx } = this.$$;
    		const props = options.props || {};

    		if (/*username*/ ctx[0] === undefined && !("username" in props)) {
    			console.warn("<Account> was created without expected prop 'username'");
    		}

    		if (/*lastUsed*/ ctx[1] === undefined && !("lastUsed" in props)) {
    			console.warn("<Account> was created without expected prop 'lastUsed'");
    		}

    		if (/*location*/ ctx[2] === undefined && !("location" in props)) {
    			console.warn("<Account> was created without expected prop 'location'");
    		}
    	}

    	get username() {
    		throw new Error("<Account>: Props cannot be read directly from the component instance unless compiling with 'accessors: true' or '<svelte:options accessors/>'");
    	}

    	set username(value) {
    		throw new Error("<Account>: Props cannot be set directly on the component instance unless compiling with 'accessors: true' or '<svelte:options accessors/>'");
    	}

    	get lastUsed() {
    		throw new Error("<Account>: Props cannot be read directly from the component instance unless compiling with 'accessors: true' or '<svelte:options accessors/>'");
    	}

    	set lastUsed(value) {
    		throw new Error("<Account>: Props cannot be set directly on the component instance unless compiling with 'accessors: true' or '<svelte:options accessors/>'");
    	}

    	get location() {
    		throw new Error("<Account>: Props cannot be read directly from the component instance unless compiling with 'accessors: true' or '<svelte:options accessors/>'");
    	}

    	set location(value) {
    		throw new Error("<Account>: Props cannot be set directly on the component instance unless compiling with 'accessors: true' or '<svelte:options accessors/>'");
    	}
    }

    /* src\elements\Logo.svelte generated by Svelte v3.38.2 */

    const file$7 = "src\\elements\\Logo.svelte";

    function create_fragment$7(ctx) {
    	let img;
    	let img_src_value;

    	const block = {
    		c: function create() {
    			img = element("img");
    			attr_dev(img, "class", "logo svelte-hsfuen");
    			if (img.src !== (img_src_value = "../img/logo.svg")) attr_dev(img, "src", img_src_value);
    			attr_dev(img, "alt", "logo");
    			add_location(img, file$7, 0, 0, 0);
    		},
    		l: function claim(nodes) {
    			throw new Error("options.hydrate only works if the component was compiled with the `hydratable: true` option");
    		},
    		m: function mount(target, anchor) {
    			insert_dev(target, img, anchor);
    		},
    		p: noop,
    		i: noop,
    		o: noop,
    		d: function destroy(detaching) {
    			if (detaching) detach_dev(img);
    		}
    	};

    	dispatch_dev("SvelteRegisterBlock", {
    		block,
    		id: create_fragment$7.name,
    		type: "component",
    		source: "",
    		ctx
    	});

    	return block;
    }

    function instance$7($$self, $$props) {
    	let { $$slots: slots = {}, $$scope } = $$props;
    	validate_slots("Logo", slots, []);
    	const writable_props = [];

    	Object.keys($$props).forEach(key => {
    		if (!~writable_props.indexOf(key) && key.slice(0, 2) !== "$$") console.warn(`<Logo> was created with unknown prop '${key}'`);
    	});

    	return [];
    }

    class Logo extends SvelteComponentDev {
    	constructor(options) {
    		super(options);
    		init(this, options, instance$7, create_fragment$7, safe_not_equal, {});

    		dispatch_dev("SvelteRegisterComponent", {
    			component: this,
    			tagName: "Logo",
    			options,
    			id: create_fragment$7.name
    		});
    	}
    }

    /* src\elements\main-buttons\ChildButton.svelte generated by Svelte v3.38.2 */
    const file$6 = "src\\elements\\main-buttons\\ChildButton.svelte";

    // (23:8) {:else}
    function create_else_block$1(ctx) {
    	let img;
    	let img_src_value;
    	let img_transition;
    	let current;

    	const block = {
    		c: function create() {
    			img = element("img");
    			if (img.src !== (img_src_value = "../img/icons/" + /*icon*/ ctx[0] + ".svg")) attr_dev(img, "src", img_src_value);
    			attr_dev(img, "alt", "icon");
    			attr_dev(img, "class", "svelte-1vslbf1");
    			add_location(img, file$6, 23, 12, 626);
    		},
    		m: function mount(target, anchor) {
    			insert_dev(target, img, anchor);
    			current = true;
    		},
    		p: function update(ctx, dirty) {
    			if (!current || dirty & /*icon*/ 1 && img.src !== (img_src_value = "../img/icons/" + /*icon*/ ctx[0] + ".svg")) {
    				attr_dev(img, "src", img_src_value);
    			}
    		},
    		i: function intro(local) {
    			if (current) return;

    			add_render_callback(() => {
    				if (!img_transition) img_transition = create_bidirectional_transition(img, fade, { duration: 200 }, true);
    				img_transition.run(1);
    			});

    			current = true;
    		},
    		o: function outro(local) {
    			if (!img_transition) img_transition = create_bidirectional_transition(img, fade, { duration: 200 }, false);
    			img_transition.run(0);
    			current = false;
    		},
    		d: function destroy(detaching) {
    			if (detaching) detach_dev(img);
    			if (detaching && img_transition) img_transition.end();
    		}
    	};

    	dispatch_dev("SvelteRegisterBlock", {
    		block,
    		id: create_else_block$1.name,
    		type: "else",
    		source: "(23:8) {:else}",
    		ctx
    	});

    	return block;
    }

    // (21:8) {#if hovered}
    function create_if_block$1(ctx) {
    	let img;
    	let img_src_value;
    	let img_transition;
    	let current;

    	const block = {
    		c: function create() {
    			img = element("img");
    			if (img.src !== (img_src_value = "../img/icons/" + /*icon*/ ctx[0] + "-hover.svg")) attr_dev(img, "src", img_src_value);
    			attr_dev(img, "alt", "icon");
    			attr_dev(img, "class", "svelte-1vslbf1");
    			add_location(img, file$6, 21, 12, 504);
    		},
    		m: function mount(target, anchor) {
    			insert_dev(target, img, anchor);
    			current = true;
    		},
    		p: function update(ctx, dirty) {
    			if (!current || dirty & /*icon*/ 1 && img.src !== (img_src_value = "../img/icons/" + /*icon*/ ctx[0] + "-hover.svg")) {
    				attr_dev(img, "src", img_src_value);
    			}
    		},
    		i: function intro(local) {
    			if (current) return;

    			add_render_callback(() => {
    				if (!img_transition) img_transition = create_bidirectional_transition(img, fade, { duration: 200 }, true);
    				img_transition.run(1);
    			});

    			current = true;
    		},
    		o: function outro(local) {
    			if (!img_transition) img_transition = create_bidirectional_transition(img, fade, { duration: 200 }, false);
    			img_transition.run(0);
    			current = false;
    		},
    		d: function destroy(detaching) {
    			if (detaching) detach_dev(img);
    			if (detaching && img_transition) img_transition.end();
    		}
    	};

    	dispatch_dev("SvelteRegisterBlock", {
    		block,
    		id: create_if_block$1.name,
    		type: "if",
    		source: "(21:8) {#if hovered}",
    		ctx
    	});

    	return block;
    }

    function create_fragment$6(ctx) {
    	let div1;
    	let tooltip;
    	let t;
    	let div0;
    	let current_block_type_index;
    	let if_block;
    	let current;
    	let mounted;
    	let dispose;

    	tooltip = new ToolTip({
    			props: { text: /*text*/ ctx[1] },
    			$$inline: true
    		});

    	const if_block_creators = [create_if_block$1, create_else_block$1];
    	const if_blocks = [];

    	function select_block_type(ctx, dirty) {
    		if (/*hovered*/ ctx[2]) return 0;
    		return 1;
    	}

    	current_block_type_index = select_block_type(ctx);
    	if_block = if_blocks[current_block_type_index] = if_block_creators[current_block_type_index](ctx);

    	const block = {
    		c: function create() {
    			div1 = element("div");
    			create_component(tooltip.$$.fragment);
    			t = space();
    			div0 = element("div");
    			if_block.c();
    			attr_dev(div0, "class", "icon svelte-1vslbf1");
    			toggle_class(div0, "hovered", /*hovered*/ ctx[2]);
    			add_location(div0, file$6, 19, 4, 435);
    			attr_dev(div1, "class", "button svelte-1vslbf1");
    			add_location(div1, file$6, 16, 0, 360);
    		},
    		l: function claim(nodes) {
    			throw new Error("options.hydrate only works if the component was compiled with the `hydratable: true` option");
    		},
    		m: function mount(target, anchor) {
    			insert_dev(target, div1, anchor);
    			mount_component(tooltip, div1, null);
    			append_dev(div1, t);
    			append_dev(div1, div0);
    			if_blocks[current_block_type_index].m(div0, null);
    			current = true;

    			if (!mounted) {
    				dispose = listen_dev(div1, "click", /*handleClick*/ ctx[3], false, false, false);
    				mounted = true;
    			}
    		},
    		p: function update(ctx, [dirty]) {
    			const tooltip_changes = {};
    			if (dirty & /*text*/ 2) tooltip_changes.text = /*text*/ ctx[1];
    			tooltip.$set(tooltip_changes);
    			let previous_block_index = current_block_type_index;
    			current_block_type_index = select_block_type(ctx);

    			if (current_block_type_index === previous_block_index) {
    				if_blocks[current_block_type_index].p(ctx, dirty);
    			} else {
    				group_outros();

    				transition_out(if_blocks[previous_block_index], 1, 1, () => {
    					if_blocks[previous_block_index] = null;
    				});

    				check_outros();
    				if_block = if_blocks[current_block_type_index];

    				if (!if_block) {
    					if_block = if_blocks[current_block_type_index] = if_block_creators[current_block_type_index](ctx);
    					if_block.c();
    				} else {
    					if_block.p(ctx, dirty);
    				}

    				transition_in(if_block, 1);
    				if_block.m(div0, null);
    			}

    			if (dirty & /*hovered*/ 4) {
    				toggle_class(div0, "hovered", /*hovered*/ ctx[2]);
    			}
    		},
    		i: function intro(local) {
    			if (current) return;
    			transition_in(tooltip.$$.fragment, local);
    			transition_in(if_block);
    			current = true;
    		},
    		o: function outro(local) {
    			transition_out(tooltip.$$.fragment, local);
    			transition_out(if_block);
    			current = false;
    		},
    		d: function destroy(detaching) {
    			if (detaching) detach_dev(div1);
    			destroy_component(tooltip);
    			if_blocks[current_block_type_index].d();
    			mounted = false;
    			dispose();
    		}
    	};

    	dispatch_dev("SvelteRegisterBlock", {
    		block,
    		id: create_fragment$6.name,
    		type: "component",
    		source: "",
    		ctx
    	});

    	return block;
    }

    function instance$6($$self, $$props, $$invalidate) {
    	let { $$slots: slots = {}, $$scope } = $$props;
    	validate_slots("ChildButton", slots, []);
    	const dispatch = createEventDispatcher();
    	let { icon } = $$props;
    	let { text } = $$props;
    	let { hovered } = $$props;

    	function handleClick(e) {
    		dispatch("click", e);
    	}

    	const writable_props = ["icon", "text", "hovered"];

    	Object.keys($$props).forEach(key => {
    		if (!~writable_props.indexOf(key) && key.slice(0, 2) !== "$$") console.warn(`<ChildButton> was created with unknown prop '${key}'`);
    	});

    	$$self.$$set = $$props => {
    		if ("icon" in $$props) $$invalidate(0, icon = $$props.icon);
    		if ("text" in $$props) $$invalidate(1, text = $$props.text);
    		if ("hovered" in $$props) $$invalidate(2, hovered = $$props.hovered);
    	};

    	$$self.$capture_state = () => ({
    		fade,
    		createEventDispatcher,
    		ToolTip,
    		dispatch,
    		icon,
    		text,
    		hovered,
    		handleClick
    	});

    	$$self.$inject_state = $$props => {
    		if ("icon" in $$props) $$invalidate(0, icon = $$props.icon);
    		if ("text" in $$props) $$invalidate(1, text = $$props.text);
    		if ("hovered" in $$props) $$invalidate(2, hovered = $$props.hovered);
    	};

    	if ($$props && "$$inject" in $$props) {
    		$$self.$inject_state($$props.$$inject);
    	}

    	return [icon, text, hovered, handleClick];
    }

    class ChildButton extends SvelteComponentDev {
    	constructor(options) {
    		super(options);
    		init(this, options, instance$6, create_fragment$6, safe_not_equal, { icon: 0, text: 1, hovered: 2 });

    		dispatch_dev("SvelteRegisterComponent", {
    			component: this,
    			tagName: "ChildButton",
    			options,
    			id: create_fragment$6.name
    		});

    		const { ctx } = this.$$;
    		const props = options.props || {};

    		if (/*icon*/ ctx[0] === undefined && !("icon" in props)) {
    			console.warn("<ChildButton> was created without expected prop 'icon'");
    		}

    		if (/*text*/ ctx[1] === undefined && !("text" in props)) {
    			console.warn("<ChildButton> was created without expected prop 'text'");
    		}

    		if (/*hovered*/ ctx[2] === undefined && !("hovered" in props)) {
    			console.warn("<ChildButton> was created without expected prop 'hovered'");
    		}
    	}

    	get icon() {
    		throw new Error("<ChildButton>: Props cannot be read directly from the component instance unless compiling with 'accessors: true' or '<svelte:options accessors/>'");
    	}

    	set icon(value) {
    		throw new Error("<ChildButton>: Props cannot be set directly on the component instance unless compiling with 'accessors: true' or '<svelte:options accessors/>'");
    	}

    	get text() {
    		throw new Error("<ChildButton>: Props cannot be read directly from the component instance unless compiling with 'accessors: true' or '<svelte:options accessors/>'");
    	}

    	set text(value) {
    		throw new Error("<ChildButton>: Props cannot be set directly on the component instance unless compiling with 'accessors: true' or '<svelte:options accessors/>'");
    	}

    	get hovered() {
    		throw new Error("<ChildButton>: Props cannot be read directly from the component instance unless compiling with 'accessors: true' or '<svelte:options accessors/>'");
    	}

    	set hovered(value) {
    		throw new Error("<ChildButton>: Props cannot be set directly on the component instance unless compiling with 'accessors: true' or '<svelte:options accessors/>'");
    	}
    }

    /* src\elements\main-buttons\MainButton.svelte generated by Svelte v3.38.2 */
    const file$5 = "src\\elements\\main-buttons\\MainButton.svelte";
    const get_default_slot_changes = dirty => ({ hovered: dirty & /*hovered*/ 4 });
    const get_default_slot_context = ctx => ({ hovered: /*hovered*/ ctx[2] });

    // (29:8) {:else}
    function create_else_block(ctx) {
    	let img;
    	let img_src_value;
    	let img_transition;
    	let current;

    	const block = {
    		c: function create() {
    			img = element("img");
    			if (img.src !== (img_src_value = "../img/icons/" + /*icon*/ ctx[1] + ".svg")) attr_dev(img, "src", img_src_value);
    			attr_dev(img, "alt", "icon");
    			attr_dev(img, "class", "svelte-1rqwtwi");
    			add_location(img, file$5, 29, 12, 750);
    		},
    		m: function mount(target, anchor) {
    			insert_dev(target, img, anchor);
    			current = true;
    		},
    		p: function update(ctx, dirty) {
    			if (!current || dirty & /*icon*/ 2 && img.src !== (img_src_value = "../img/icons/" + /*icon*/ ctx[1] + ".svg")) {
    				attr_dev(img, "src", img_src_value);
    			}
    		},
    		i: function intro(local) {
    			if (current) return;

    			add_render_callback(() => {
    				if (!img_transition) img_transition = create_bidirectional_transition(img, fade, { duration: 200 }, true);
    				img_transition.run(1);
    			});

    			current = true;
    		},
    		o: function outro(local) {
    			if (!img_transition) img_transition = create_bidirectional_transition(img, fade, { duration: 200 }, false);
    			img_transition.run(0);
    			current = false;
    		},
    		d: function destroy(detaching) {
    			if (detaching) detach_dev(img);
    			if (detaching && img_transition) img_transition.end();
    		}
    	};

    	dispatch_dev("SvelteRegisterBlock", {
    		block,
    		id: create_else_block.name,
    		type: "else",
    		source: "(29:8) {:else}",
    		ctx
    	});

    	return block;
    }

    // (27:8) {#if hovered}
    function create_if_block(ctx) {
    	let img;
    	let img_src_value;
    	let img_transition;
    	let current;

    	const block = {
    		c: function create() {
    			img = element("img");
    			if (img.src !== (img_src_value = "../img/icons/" + /*icon*/ ctx[1] + "-hover.svg")) attr_dev(img, "src", img_src_value);
    			attr_dev(img, "alt", "icon");
    			attr_dev(img, "class", "svelte-1rqwtwi");
    			add_location(img, file$5, 27, 12, 628);
    		},
    		m: function mount(target, anchor) {
    			insert_dev(target, img, anchor);
    			current = true;
    		},
    		p: function update(ctx, dirty) {
    			if (!current || dirty & /*icon*/ 2 && img.src !== (img_src_value = "../img/icons/" + /*icon*/ ctx[1] + "-hover.svg")) {
    				attr_dev(img, "src", img_src_value);
    			}
    		},
    		i: function intro(local) {
    			if (current) return;

    			add_render_callback(() => {
    				if (!img_transition) img_transition = create_bidirectional_transition(img, fade, { duration: 200 }, true);
    				img_transition.run(1);
    			});

    			current = true;
    		},
    		o: function outro(local) {
    			if (!img_transition) img_transition = create_bidirectional_transition(img, fade, { duration: 200 }, false);
    			img_transition.run(0);
    			current = false;
    		},
    		d: function destroy(detaching) {
    			if (detaching) detach_dev(img);
    			if (detaching && img_transition) img_transition.end();
    		}
    	};

    	dispatch_dev("SvelteRegisterBlock", {
    		block,
    		id: create_if_block.name,
    		type: "if",
    		source: "(27:8) {#if hovered}",
    		ctx
    	});

    	return block;
    }

    function create_fragment$5(ctx) {
    	let div2;
    	let div0;
    	let current_block_type_index;
    	let if_block;
    	let t0;
    	let div1;
    	let t1;
    	let t2;
    	let current;
    	let mounted;
    	let dispose;
    	const if_block_creators = [create_if_block, create_else_block];
    	const if_blocks = [];

    	function select_block_type(ctx, dirty) {
    		if (/*hovered*/ ctx[2]) return 0;
    		return 1;
    	}

    	current_block_type_index = select_block_type(ctx);
    	if_block = if_blocks[current_block_type_index] = if_block_creators[current_block_type_index](ctx);
    	const default_slot_template = /*#slots*/ ctx[7].default;
    	const default_slot = create_slot(default_slot_template, ctx, /*$$scope*/ ctx[6], get_default_slot_context);

    	const block = {
    		c: function create() {
    			div2 = element("div");
    			div0 = element("div");
    			if_block.c();
    			t0 = space();
    			div1 = element("div");
    			t1 = text(/*text*/ ctx[0]);
    			t2 = space();
    			if (default_slot) default_slot.c();
    			attr_dev(div0, "class", "icon svelte-1rqwtwi");
    			add_location(div0, file$5, 25, 4, 573);
    			attr_dev(div1, "class", "text svelte-1rqwtwi");
    			add_location(div1, file$5, 32, 4, 868);
    			attr_dev(div2, "class", "button svelte-1rqwtwi");
    			add_location(div2, file$5, 24, 0, 458);
    		},
    		l: function claim(nodes) {
    			throw new Error("options.hydrate only works if the component was compiled with the `hydratable: true` option");
    		},
    		m: function mount(target, anchor) {
    			insert_dev(target, div2, anchor);
    			append_dev(div2, div0);
    			if_blocks[current_block_type_index].m(div0, null);
    			append_dev(div2, t0);
    			append_dev(div2, div1);
    			append_dev(div1, t1);
    			append_dev(div2, t2);

    			if (default_slot) {
    				default_slot.m(div2, null);
    			}

    			current = true;

    			if (!mounted) {
    				dispose = [
    					listen_dev(div2, "mouseenter", /*handleMouseEnter*/ ctx[3], false, false, false),
    					listen_dev(div2, "mouseleave", /*handleMouseLeave*/ ctx[4], false, false, false),
    					listen_dev(div2, "click", /*handleClick*/ ctx[5], false, false, false)
    				];

    				mounted = true;
    			}
    		},
    		p: function update(ctx, [dirty]) {
    			let previous_block_index = current_block_type_index;
    			current_block_type_index = select_block_type(ctx);

    			if (current_block_type_index === previous_block_index) {
    				if_blocks[current_block_type_index].p(ctx, dirty);
    			} else {
    				group_outros();

    				transition_out(if_blocks[previous_block_index], 1, 1, () => {
    					if_blocks[previous_block_index] = null;
    				});

    				check_outros();
    				if_block = if_blocks[current_block_type_index];

    				if (!if_block) {
    					if_block = if_blocks[current_block_type_index] = if_block_creators[current_block_type_index](ctx);
    					if_block.c();
    				} else {
    					if_block.p(ctx, dirty);
    				}

    				transition_in(if_block, 1);
    				if_block.m(div0, null);
    			}

    			if (!current || dirty & /*text*/ 1) set_data_dev(t1, /*text*/ ctx[0]);

    			if (default_slot) {
    				if (default_slot.p && (!current || dirty & /*$$scope, hovered*/ 68)) {
    					update_slot(default_slot, default_slot_template, ctx, /*$$scope*/ ctx[6], dirty, get_default_slot_changes, get_default_slot_context);
    				}
    			}
    		},
    		i: function intro(local) {
    			if (current) return;
    			transition_in(if_block);
    			transition_in(default_slot, local);
    			current = true;
    		},
    		o: function outro(local) {
    			transition_out(if_block);
    			transition_out(default_slot, local);
    			current = false;
    		},
    		d: function destroy(detaching) {
    			if (detaching) detach_dev(div2);
    			if_blocks[current_block_type_index].d();
    			if (default_slot) default_slot.d(detaching);
    			mounted = false;
    			run_all(dispose);
    		}
    	};

    	dispatch_dev("SvelteRegisterBlock", {
    		block,
    		id: create_fragment$5.name,
    		type: "component",
    		source: "",
    		ctx
    	});

    	return block;
    }

    function instance$5($$self, $$props, $$invalidate) {
    	let { $$slots: slots = {}, $$scope } = $$props;
    	validate_slots("MainButton", slots, ['default']);
    	const dispatch = createEventDispatcher();
    	let { text } = $$props;
    	let { icon } = $$props;
    	let hovered = false;

    	function handleMouseEnter(e) {
    		$$invalidate(2, hovered = true);
    	}

    	function handleMouseLeave(e) {
    		$$invalidate(2, hovered = false);
    	}

    	function handleClick(e) {
    		dispatch("click", e);
    	}

    	const writable_props = ["text", "icon"];

    	Object.keys($$props).forEach(key => {
    		if (!~writable_props.indexOf(key) && key.slice(0, 2) !== "$$") console.warn(`<MainButton> was created with unknown prop '${key}'`);
    	});

    	$$self.$$set = $$props => {
    		if ("text" in $$props) $$invalidate(0, text = $$props.text);
    		if ("icon" in $$props) $$invalidate(1, icon = $$props.icon);
    		if ("$$scope" in $$props) $$invalidate(6, $$scope = $$props.$$scope);
    	};

    	$$self.$capture_state = () => ({
    		fade,
    		createEventDispatcher,
    		dispatch,
    		text,
    		icon,
    		hovered,
    		handleMouseEnter,
    		handleMouseLeave,
    		handleClick
    	});

    	$$self.$inject_state = $$props => {
    		if ("text" in $$props) $$invalidate(0, text = $$props.text);
    		if ("icon" in $$props) $$invalidate(1, icon = $$props.icon);
    		if ("hovered" in $$props) $$invalidate(2, hovered = $$props.hovered);
    	};

    	if ($$props && "$$inject" in $$props) {
    		$$self.$inject_state($$props.$$inject);
    	}

    	return [
    		text,
    		icon,
    		hovered,
    		handleMouseEnter,
    		handleMouseLeave,
    		handleClick,
    		$$scope,
    		slots
    	];
    }

    class MainButton extends SvelteComponentDev {
    	constructor(options) {
    		super(options);
    		init(this, options, instance$5, create_fragment$5, safe_not_equal, { text: 0, icon: 1 });

    		dispatch_dev("SvelteRegisterComponent", {
    			component: this,
    			tagName: "MainButton",
    			options,
    			id: create_fragment$5.name
    		});

    		const { ctx } = this.$$;
    		const props = options.props || {};

    		if (/*text*/ ctx[0] === undefined && !("text" in props)) {
    			console.warn("<MainButton> was created without expected prop 'text'");
    		}

    		if (/*icon*/ ctx[1] === undefined && !("icon" in props)) {
    			console.warn("<MainButton> was created without expected prop 'icon'");
    		}
    	}

    	get text() {
    		throw new Error("<MainButton>: Props cannot be read directly from the component instance unless compiling with 'accessors: true' or '<svelte:options accessors/>'");
    	}

    	set text(value) {
    		throw new Error("<MainButton>: Props cannot be set directly on the component instance unless compiling with 'accessors: true' or '<svelte:options accessors/>'");
    	}

    	get icon() {
    		throw new Error("<MainButton>: Props cannot be read directly from the component instance unless compiling with 'accessors: true' or '<svelte:options accessors/>'");
    	}

    	set icon(value) {
    		throw new Error("<MainButton>: Props cannot be set directly on the component instance unless compiling with 'accessors: true' or '<svelte:options accessors/>'");
    	}
    }

    /* src\elements\main-buttons\MainButtons.svelte generated by Svelte v3.38.2 */

    const file$4 = "src\\elements\\main-buttons\\MainButtons.svelte";

    function create_fragment$4(ctx) {
    	let div;
    	let current;
    	const default_slot_template = /*#slots*/ ctx[1].default;
    	const default_slot = create_slot(default_slot_template, ctx, /*$$scope*/ ctx[0], null);

    	const block = {
    		c: function create() {
    			div = element("div");
    			if (default_slot) default_slot.c();
    			attr_dev(div, "class", "buttons svelte-1np6dk1");
    			add_location(div, file$4, 0, 0, 0);
    		},
    		l: function claim(nodes) {
    			throw new Error("options.hydrate only works if the component was compiled with the `hydratable: true` option");
    		},
    		m: function mount(target, anchor) {
    			insert_dev(target, div, anchor);

    			if (default_slot) {
    				default_slot.m(div, null);
    			}

    			current = true;
    		},
    		p: function update(ctx, [dirty]) {
    			if (default_slot) {
    				if (default_slot.p && (!current || dirty & /*$$scope*/ 1)) {
    					update_slot(default_slot, default_slot_template, ctx, /*$$scope*/ ctx[0], dirty, null, null);
    				}
    			}
    		},
    		i: function intro(local) {
    			if (current) return;
    			transition_in(default_slot, local);
    			current = true;
    		},
    		o: function outro(local) {
    			transition_out(default_slot, local);
    			current = false;
    		},
    		d: function destroy(detaching) {
    			if (detaching) detach_dev(div);
    			if (default_slot) default_slot.d(detaching);
    		}
    	};

    	dispatch_dev("SvelteRegisterBlock", {
    		block,
    		id: create_fragment$4.name,
    		type: "component",
    		source: "",
    		ctx
    	});

    	return block;
    }

    function instance$4($$self, $$props, $$invalidate) {
    	let { $$slots: slots = {}, $$scope } = $$props;
    	validate_slots("MainButtons", slots, ['default']);
    	const writable_props = [];

    	Object.keys($$props).forEach(key => {
    		if (!~writable_props.indexOf(key) && key.slice(0, 2) !== "$$") console.warn(`<MainButtons> was created with unknown prop '${key}'`);
    	});

    	$$self.$$set = $$props => {
    		if ("$$scope" in $$props) $$invalidate(0, $$scope = $$props.$$scope);
    	};

    	return [$$scope, slots];
    }

    class MainButtons extends SvelteComponentDev {
    	constructor(options) {
    		super(options);
    		init(this, options, instance$4, create_fragment$4, safe_not_equal, {});

    		dispatch_dev("SvelteRegisterComponent", {
    			component: this,
    			tagName: "MainButtons",
    			options,
    			id: create_fragment$4.name
    		});
    	}
    }

    /* src\elements\secondary-buttons\IconButton.svelte generated by Svelte v3.38.2 */
    const file$3 = "src\\elements\\secondary-buttons\\IconButton.svelte";

    function create_fragment$3(ctx) {
    	let div1;
    	let tooltip;
    	let t;
    	let div0;
    	let img;
    	let img_src_value;
    	let current;
    	let mounted;
    	let dispose;

    	tooltip = new ToolTip({
    			props: { text: /*text*/ ctx[0] },
    			$$inline: true
    		});

    	const block = {
    		c: function create() {
    			div1 = element("div");
    			create_component(tooltip.$$.fragment);
    			t = space();
    			div0 = element("div");
    			img = element("img");
    			if (img.src !== (img_src_value = "../img/icons/" + /*icon*/ ctx[1] + ".svg")) attr_dev(img, "src", img_src_value);
    			attr_dev(img, "alt", "icon");
    			add_location(img, file$3, 18, 8, 395);
    			attr_dev(div0, "class", "icon svelte-qbs8wg");
    			add_location(div0, file$3, 17, 4, 367);
    			attr_dev(div1, "class", "button svelte-qbs8wg");
    			add_location(div1, file$3, 14, 0, 292);
    		},
    		l: function claim(nodes) {
    			throw new Error("options.hydrate only works if the component was compiled with the `hydratable: true` option");
    		},
    		m: function mount(target, anchor) {
    			insert_dev(target, div1, anchor);
    			mount_component(tooltip, div1, null);
    			append_dev(div1, t);
    			append_dev(div1, div0);
    			append_dev(div0, img);
    			current = true;

    			if (!mounted) {
    				dispose = listen_dev(div1, "click", /*handleClick*/ ctx[2], false, false, false);
    				mounted = true;
    			}
    		},
    		p: function update(ctx, [dirty]) {
    			const tooltip_changes = {};
    			if (dirty & /*text*/ 1) tooltip_changes.text = /*text*/ ctx[0];
    			tooltip.$set(tooltip_changes);

    			if (!current || dirty & /*icon*/ 2 && img.src !== (img_src_value = "../img/icons/" + /*icon*/ ctx[1] + ".svg")) {
    				attr_dev(img, "src", img_src_value);
    			}
    		},
    		i: function intro(local) {
    			if (current) return;
    			transition_in(tooltip.$$.fragment, local);
    			current = true;
    		},
    		o: function outro(local) {
    			transition_out(tooltip.$$.fragment, local);
    			current = false;
    		},
    		d: function destroy(detaching) {
    			if (detaching) detach_dev(div1);
    			destroy_component(tooltip);
    			mounted = false;
    			dispose();
    		}
    	};

    	dispatch_dev("SvelteRegisterBlock", {
    		block,
    		id: create_fragment$3.name,
    		type: "component",
    		source: "",
    		ctx
    	});

    	return block;
    }

    function instance$3($$self, $$props, $$invalidate) {
    	let { $$slots: slots = {}, $$scope } = $$props;
    	validate_slots("IconButton", slots, []);
    	let { text } = $$props;
    	let { icon } = $$props;
    	const dispatch = createEventDispatcher();

    	function handleClick(e) {
    		dispatch("click", e);
    	}

    	const writable_props = ["text", "icon"];

    	Object.keys($$props).forEach(key => {
    		if (!~writable_props.indexOf(key) && key.slice(0, 2) !== "$$") console.warn(`<IconButton> was created with unknown prop '${key}'`);
    	});

    	$$self.$$set = $$props => {
    		if ("text" in $$props) $$invalidate(0, text = $$props.text);
    		if ("icon" in $$props) $$invalidate(1, icon = $$props.icon);
    	};

    	$$self.$capture_state = () => ({
    		createEventDispatcher,
    		ToolTip,
    		text,
    		icon,
    		dispatch,
    		handleClick
    	});

    	$$self.$inject_state = $$props => {
    		if ("text" in $$props) $$invalidate(0, text = $$props.text);
    		if ("icon" in $$props) $$invalidate(1, icon = $$props.icon);
    	};

    	if ($$props && "$$inject" in $$props) {
    		$$self.$inject_state($$props.$$inject);
    	}

    	return [text, icon, handleClick];
    }

    class IconButton extends SvelteComponentDev {
    	constructor(options) {
    		super(options);
    		init(this, options, instance$3, create_fragment$3, safe_not_equal, { text: 0, icon: 1 });

    		dispatch_dev("SvelteRegisterComponent", {
    			component: this,
    			tagName: "IconButton",
    			options,
    			id: create_fragment$3.name
    		});

    		const { ctx } = this.$$;
    		const props = options.props || {};

    		if (/*text*/ ctx[0] === undefined && !("text" in props)) {
    			console.warn("<IconButton> was created without expected prop 'text'");
    		}

    		if (/*icon*/ ctx[1] === undefined && !("icon" in props)) {
    			console.warn("<IconButton> was created without expected prop 'icon'");
    		}
    	}

    	get text() {
    		throw new Error("<IconButton>: Props cannot be read directly from the component instance unless compiling with 'accessors: true' or '<svelte:options accessors/>'");
    	}

    	set text(value) {
    		throw new Error("<IconButton>: Props cannot be set directly on the component instance unless compiling with 'accessors: true' or '<svelte:options accessors/>'");
    	}

    	get icon() {
    		throw new Error("<IconButton>: Props cannot be read directly from the component instance unless compiling with 'accessors: true' or '<svelte:options accessors/>'");
    	}

    	set icon(value) {
    		throw new Error("<IconButton>: Props cannot be set directly on the component instance unless compiling with 'accessors: true' or '<svelte:options accessors/>'");
    	}
    }

    /* src\elements\secondary-buttons\IconTextButton.svelte generated by Svelte v3.38.2 */
    const file$2 = "src\\elements\\secondary-buttons\\IconTextButton.svelte";

    function create_fragment$2(ctx) {
    	let div2;
    	let div0;
    	let img;
    	let img_src_value;
    	let t0;
    	let div1;
    	let t1;
    	let mounted;
    	let dispose;

    	const block = {
    		c: function create() {
    			div2 = element("div");
    			div0 = element("div");
    			img = element("img");
    			t0 = space();
    			div1 = element("div");
    			t1 = text(/*text*/ ctx[0]);
    			if (img.src !== (img_src_value = "../img/icons/" + /*icon*/ ctx[1] + ".svg")) attr_dev(img, "src", img_src_value);
    			attr_dev(img, "alt", "icon");
    			add_location(img, file$2, 16, 8, 366);
    			attr_dev(div0, "class", "icon svelte-uwyc2x");
    			add_location(div0, file$2, 15, 4, 338);
    			attr_dev(div1, "class", "text svelte-uwyc2x");
    			add_location(div1, file$2, 18, 4, 431);
    			attr_dev(div2, "class", "button svelte-uwyc2x");
    			add_location(div2, file$2, 14, 0, 289);
    		},
    		l: function claim(nodes) {
    			throw new Error("options.hydrate only works if the component was compiled with the `hydratable: true` option");
    		},
    		m: function mount(target, anchor) {
    			insert_dev(target, div2, anchor);
    			append_dev(div2, div0);
    			append_dev(div0, img);
    			append_dev(div2, t0);
    			append_dev(div2, div1);
    			append_dev(div1, t1);

    			if (!mounted) {
    				dispose = listen_dev(div2, "click", /*handleClick*/ ctx[2], false, false, false);
    				mounted = true;
    			}
    		},
    		p: function update(ctx, [dirty]) {
    			if (dirty & /*icon*/ 2 && img.src !== (img_src_value = "../img/icons/" + /*icon*/ ctx[1] + ".svg")) {
    				attr_dev(img, "src", img_src_value);
    			}

    			if (dirty & /*text*/ 1) set_data_dev(t1, /*text*/ ctx[0]);
    		},
    		i: noop,
    		o: noop,
    		d: function destroy(detaching) {
    			if (detaching) detach_dev(div2);
    			mounted = false;
    			dispose();
    		}
    	};

    	dispatch_dev("SvelteRegisterBlock", {
    		block,
    		id: create_fragment$2.name,
    		type: "component",
    		source: "",
    		ctx
    	});

    	return block;
    }

    function instance$2($$self, $$props, $$invalidate) {
    	let { $$slots: slots = {}, $$scope } = $$props;
    	validate_slots("IconTextButton", slots, []);
    	let { text } = $$props;
    	let { icon } = $$props;
    	const dispatch = createEventDispatcher();

    	function handleClick(e) {
    		dispatch("click", e);
    	}

    	const writable_props = ["text", "icon"];

    	Object.keys($$props).forEach(key => {
    		if (!~writable_props.indexOf(key) && key.slice(0, 2) !== "$$") console.warn(`<IconTextButton> was created with unknown prop '${key}'`);
    	});

    	$$self.$$set = $$props => {
    		if ("text" in $$props) $$invalidate(0, text = $$props.text);
    		if ("icon" in $$props) $$invalidate(1, icon = $$props.icon);
    	};

    	$$self.$capture_state = () => ({
    		fade,
    		createEventDispatcher,
    		text,
    		icon,
    		dispatch,
    		handleClick
    	});

    	$$self.$inject_state = $$props => {
    		if ("text" in $$props) $$invalidate(0, text = $$props.text);
    		if ("icon" in $$props) $$invalidate(1, icon = $$props.icon);
    	};

    	if ($$props && "$$inject" in $$props) {
    		$$self.$inject_state($$props.$$inject);
    	}

    	return [text, icon, handleClick];
    }

    class IconTextButton extends SvelteComponentDev {
    	constructor(options) {
    		super(options);
    		init(this, options, instance$2, create_fragment$2, safe_not_equal, { text: 0, icon: 1 });

    		dispatch_dev("SvelteRegisterComponent", {
    			component: this,
    			tagName: "IconTextButton",
    			options,
    			id: create_fragment$2.name
    		});

    		const { ctx } = this.$$;
    		const props = options.props || {};

    		if (/*text*/ ctx[0] === undefined && !("text" in props)) {
    			console.warn("<IconTextButton> was created without expected prop 'text'");
    		}

    		if (/*icon*/ ctx[1] === undefined && !("icon" in props)) {
    			console.warn("<IconTextButton> was created without expected prop 'icon'");
    		}
    	}

    	get text() {
    		throw new Error("<IconTextButton>: Props cannot be read directly from the component instance unless compiling with 'accessors: true' or '<svelte:options accessors/>'");
    	}

    	set text(value) {
    		throw new Error("<IconTextButton>: Props cannot be set directly on the component instance unless compiling with 'accessors: true' or '<svelte:options accessors/>'");
    	}

    	get icon() {
    		throw new Error("<IconTextButton>: Props cannot be read directly from the component instance unless compiling with 'accessors: true' or '<svelte:options accessors/>'");
    	}

    	set icon(value) {
    		throw new Error("<IconTextButton>: Props cannot be set directly on the component instance unless compiling with 'accessors: true' or '<svelte:options accessors/>'");
    	}
    }

    /* src\elements\secondary-buttons\SecondaryButtons.svelte generated by Svelte v3.38.2 */

    const file$1 = "src\\elements\\secondary-buttons\\SecondaryButtons.svelte";

    function create_fragment$1(ctx) {
    	let div;
    	let current;
    	const default_slot_template = /*#slots*/ ctx[1].default;
    	const default_slot = create_slot(default_slot_template, ctx, /*$$scope*/ ctx[0], null);

    	const block = {
    		c: function create() {
    			div = element("div");
    			if (default_slot) default_slot.c();
    			attr_dev(div, "class", "buttons svelte-1urs5sa");
    			add_location(div, file$1, 0, 0, 0);
    		},
    		l: function claim(nodes) {
    			throw new Error("options.hydrate only works if the component was compiled with the `hydratable: true` option");
    		},
    		m: function mount(target, anchor) {
    			insert_dev(target, div, anchor);

    			if (default_slot) {
    				default_slot.m(div, null);
    			}

    			current = true;
    		},
    		p: function update(ctx, [dirty]) {
    			if (default_slot) {
    				if (default_slot.p && (!current || dirty & /*$$scope*/ 1)) {
    					update_slot(default_slot, default_slot_template, ctx, /*$$scope*/ ctx[0], dirty, null, null);
    				}
    			}
    		},
    		i: function intro(local) {
    			if (current) return;
    			transition_in(default_slot, local);
    			current = true;
    		},
    		o: function outro(local) {
    			transition_out(default_slot, local);
    			current = false;
    		},
    		d: function destroy(detaching) {
    			if (detaching) detach_dev(div);
    			if (default_slot) default_slot.d(detaching);
    		}
    	};

    	dispatch_dev("SvelteRegisterBlock", {
    		block,
    		id: create_fragment$1.name,
    		type: "component",
    		source: "",
    		ctx
    	});

    	return block;
    }

    function instance$1($$self, $$props, $$invalidate) {
    	let { $$slots: slots = {}, $$scope } = $$props;
    	validate_slots("SecondaryButtons", slots, ['default']);
    	const writable_props = [];

    	Object.keys($$props).forEach(key => {
    		if (!~writable_props.indexOf(key) && key.slice(0, 2) !== "$$") console.warn(`<SecondaryButtons> was created with unknown prop '${key}'`);
    	});

    	$$self.$$set = $$props => {
    		if ("$$scope" in $$props) $$invalidate(0, $$scope = $$props.$$scope);
    	};

    	return [$$scope, slots];
    }

    class SecondaryButtons extends SvelteComponentDev {
    	constructor(options) {
    		super(options);
    		init(this, options, instance$1, create_fragment$1, safe_not_equal, {});

    		dispatch_dev("SvelteRegisterComponent", {
    			component: this,
    			tagName: "SecondaryButtons",
    			options,
    			id: create_fragment$1.name
    		});
    	}
    }

    /* src\MainMenu.svelte generated by Svelte v3.38.2 */
    const file = "src\\MainMenu.svelte";

    // (42:12) <MainButton text="Multiplayer" icon="multiplayer" on:click={openMultiplayer} let:hovered>
    function create_default_slot_3(ctx) {
    	let childbutton;
    	let current;

    	childbutton = new ChildButton({
    			props: {
    				text: "Realms",
    				icon: "realms",
    				hovered: /*hovered*/ ctx[0]
    			},
    			$$inline: true
    		});

    	const block = {
    		c: function create() {
    			create_component(childbutton.$$.fragment);
    		},
    		m: function mount(target, anchor) {
    			mount_component(childbutton, target, anchor);
    			current = true;
    		},
    		p: function update(ctx, dirty) {
    			const childbutton_changes = {};
    			if (dirty & /*hovered*/ 1) childbutton_changes.hovered = /*hovered*/ ctx[0];
    			childbutton.$set(childbutton_changes);
    		},
    		i: function intro(local) {
    			if (current) return;
    			transition_in(childbutton.$$.fragment, local);
    			current = true;
    		},
    		o: function outro(local) {
    			transition_out(childbutton.$$.fragment, local);
    			current = false;
    		},
    		d: function destroy(detaching) {
    			destroy_component(childbutton, detaching);
    		}
    	};

    	dispatch_dev("SvelteRegisterBlock", {
    		block,
    		id: create_default_slot_3.name,
    		type: "slot",
    		source: "(42:12) <MainButton text=\\\"Multiplayer\\\" icon=\\\"multiplayer\\\" on:click={openMultiplayer} let:hovered>",
    		ctx
    	});

    	return block;
    }

    // (40:8) <MainButtons>
    function create_default_slot_2(ctx) {
    	let mainbutton0;
    	let t0;
    	let mainbutton1;
    	let t1;
    	let mainbutton2;
    	let t2;
    	let mainbutton3;
    	let current;

    	mainbutton0 = new MainButton({
    			props: {
    				text: "Singleplayer",
    				icon: "singleplayer"
    			},
    			$$inline: true
    		});

    	mainbutton0.$on("click", openSingleplayer);

    	mainbutton1 = new MainButton({
    			props: {
    				text: "Multiplayer",
    				icon: "multiplayer",
    				$$slots: {
    					default: [
    						create_default_slot_3,
    						({ hovered }) => ({ 0: hovered }),
    						({ hovered }) => hovered ? 1 : 0
    					]
    				},
    				$$scope: { ctx }
    			},
    			$$inline: true
    		});

    	mainbutton1.$on("click", openMultiplayer);

    	mainbutton2 = new MainButton({
    			props: { text: "Customize", icon: "customize" },
    			$$inline: true
    		});

    	mainbutton3 = new MainButton({
    			props: { text: "Options", icon: "options" },
    			$$inline: true
    		});

    	mainbutton3.$on("click", openOptions);

    	const block = {
    		c: function create() {
    			create_component(mainbutton0.$$.fragment);
    			t0 = space();
    			create_component(mainbutton1.$$.fragment);
    			t1 = space();
    			create_component(mainbutton2.$$.fragment);
    			t2 = space();
    			create_component(mainbutton3.$$.fragment);
    		},
    		m: function mount(target, anchor) {
    			mount_component(mainbutton0, target, anchor);
    			insert_dev(target, t0, anchor);
    			mount_component(mainbutton1, target, anchor);
    			insert_dev(target, t1, anchor);
    			mount_component(mainbutton2, target, anchor);
    			insert_dev(target, t2, anchor);
    			mount_component(mainbutton3, target, anchor);
    			current = true;
    		},
    		p: function update(ctx, dirty) {
    			const mainbutton1_changes = {};

    			if (dirty & /*$$scope, hovered*/ 3) {
    				mainbutton1_changes.$$scope = { dirty, ctx };
    			}

    			mainbutton1.$set(mainbutton1_changes);
    		},
    		i: function intro(local) {
    			if (current) return;
    			transition_in(mainbutton0.$$.fragment, local);
    			transition_in(mainbutton1.$$.fragment, local);
    			transition_in(mainbutton2.$$.fragment, local);
    			transition_in(mainbutton3.$$.fragment, local);
    			current = true;
    		},
    		o: function outro(local) {
    			transition_out(mainbutton0.$$.fragment, local);
    			transition_out(mainbutton1.$$.fragment, local);
    			transition_out(mainbutton2.$$.fragment, local);
    			transition_out(mainbutton3.$$.fragment, local);
    			current = false;
    		},
    		d: function destroy(detaching) {
    			destroy_component(mainbutton0, detaching);
    			if (detaching) detach_dev(t0);
    			destroy_component(mainbutton1, detaching);
    			if (detaching) detach_dev(t1);
    			destroy_component(mainbutton2, detaching);
    			if (detaching) detach_dev(t2);
    			destroy_component(mainbutton3, detaching);
    		}
    	};

    	dispatch_dev("SvelteRegisterBlock", {
    		block,
    		id: create_default_slot_2.name,
    		type: "slot",
    		source: "(40:8) <MainButtons>",
    		ctx
    	});

    	return block;
    }

    // (50:12) <SecondaryButtons>
    function create_default_slot_1(ctx) {
    	let icontextbutton0;
    	let t;
    	let icontextbutton1;
    	let current;

    	icontextbutton0 = new IconTextButton({
    			props: {
    				text: "Change Background",
    				icon: "change-background"
    			},
    			$$inline: true
    		});

    	icontextbutton1 = new IconTextButton({
    			props: { text: "Exit", icon: "exit" },
    			$$inline: true
    		});

    	icontextbutton1.$on("click", scheduleStop);

    	const block = {
    		c: function create() {
    			create_component(icontextbutton0.$$.fragment);
    			t = space();
    			create_component(icontextbutton1.$$.fragment);
    		},
    		m: function mount(target, anchor) {
    			mount_component(icontextbutton0, target, anchor);
    			insert_dev(target, t, anchor);
    			mount_component(icontextbutton1, target, anchor);
    			current = true;
    		},
    		p: noop,
    		i: function intro(local) {
    			if (current) return;
    			transition_in(icontextbutton0.$$.fragment, local);
    			transition_in(icontextbutton1.$$.fragment, local);
    			current = true;
    		},
    		o: function outro(local) {
    			transition_out(icontextbutton0.$$.fragment, local);
    			transition_out(icontextbutton1.$$.fragment, local);
    			current = false;
    		},
    		d: function destroy(detaching) {
    			destroy_component(icontextbutton0, detaching);
    			if (detaching) detach_dev(t);
    			destroy_component(icontextbutton1, detaching);
    		}
    	};

    	dispatch_dev("SvelteRegisterBlock", {
    		block,
    		id: create_default_slot_1.name,
    		type: "slot",
    		source: "(50:12) <SecondaryButtons>",
    		ctx
    	});

    	return block;
    }

    // (57:12) <SecondaryButtons>
    function create_default_slot(ctx) {
    	let iconbutton0;
    	let t0;
    	let iconbutton1;
    	let t1;
    	let iconbutton2;
    	let t2;
    	let iconbutton3;
    	let t3;
    	let iconbutton4;
    	let t4;
    	let icontextbutton;
    	let current;

    	iconbutton0 = new IconButton({
    			props: { text: "Forum", icon: "nodebb" },
    			$$inline: true
    		});

    	iconbutton1 = new IconButton({
    			props: { text: "GitHub", icon: "github" },
    			$$inline: true
    		});

    	iconbutton2 = new IconButton({
    			props: { text: "Guilded", icon: "guilded" },
    			$$inline: true
    		});

    	iconbutton3 = new IconButton({
    			props: { text: "Twitter", icon: "twitter" },
    			$$inline: true
    		});

    	iconbutton4 = new IconButton({
    			props: { text: "YouTube", icon: "youtube" },
    			$$inline: true
    		});

    	icontextbutton = new IconTextButton({
    			props: {
    				text: "liquidbounce.net",
    				icon: "liquidbounce.net"
    			},
    			$$inline: true
    		});

    	const block = {
    		c: function create() {
    			create_component(iconbutton0.$$.fragment);
    			t0 = space();
    			create_component(iconbutton1.$$.fragment);
    			t1 = space();
    			create_component(iconbutton2.$$.fragment);
    			t2 = space();
    			create_component(iconbutton3.$$.fragment);
    			t3 = space();
    			create_component(iconbutton4.$$.fragment);
    			t4 = space();
    			create_component(icontextbutton.$$.fragment);
    		},
    		m: function mount(target, anchor) {
    			mount_component(iconbutton0, target, anchor);
    			insert_dev(target, t0, anchor);
    			mount_component(iconbutton1, target, anchor);
    			insert_dev(target, t1, anchor);
    			mount_component(iconbutton2, target, anchor);
    			insert_dev(target, t2, anchor);
    			mount_component(iconbutton3, target, anchor);
    			insert_dev(target, t3, anchor);
    			mount_component(iconbutton4, target, anchor);
    			insert_dev(target, t4, anchor);
    			mount_component(icontextbutton, target, anchor);
    			current = true;
    		},
    		p: noop,
    		i: function intro(local) {
    			if (current) return;
    			transition_in(iconbutton0.$$.fragment, local);
    			transition_in(iconbutton1.$$.fragment, local);
    			transition_in(iconbutton2.$$.fragment, local);
    			transition_in(iconbutton3.$$.fragment, local);
    			transition_in(iconbutton4.$$.fragment, local);
    			transition_in(icontextbutton.$$.fragment, local);
    			current = true;
    		},
    		o: function outro(local) {
    			transition_out(iconbutton0.$$.fragment, local);
    			transition_out(iconbutton1.$$.fragment, local);
    			transition_out(iconbutton2.$$.fragment, local);
    			transition_out(iconbutton3.$$.fragment, local);
    			transition_out(iconbutton4.$$.fragment, local);
    			transition_out(icontextbutton.$$.fragment, local);
    			current = false;
    		},
    		d: function destroy(detaching) {
    			destroy_component(iconbutton0, detaching);
    			if (detaching) detach_dev(t0);
    			destroy_component(iconbutton1, detaching);
    			if (detaching) detach_dev(t1);
    			destroy_component(iconbutton2, detaching);
    			if (detaching) detach_dev(t2);
    			destroy_component(iconbutton3, detaching);
    			if (detaching) detach_dev(t3);
    			destroy_component(iconbutton4, detaching);
    			if (detaching) detach_dev(t4);
    			destroy_component(icontextbutton, detaching);
    		}
    	};

    	dispatch_dev("SvelteRegisterBlock", {
    		block,
    		id: create_default_slot.name,
    		type: "slot",
    		source: "(57:12) <SecondaryButtons>",
    		ctx
    	});

    	return block;
    }

    function create_fragment(ctx) {
    	let main;
    	let div2;
    	let logo;
    	let t0;
    	let account;
    	let t1;
    	let mainbuttons;
    	let t2;
    	let div0;
    	let secondarybuttons0;
    	let t3;
    	let div1;
    	let secondarybuttons1;
    	let current;
    	logo = new Logo({ $$inline: true });

    	account = new Account({
    			props: {
    				username: "heafie",
    				location: "de",
    				lastUsed: "2021-05-07"
    			},
    			$$inline: true
    		});

    	account.$on("proxyManagerClick", openProxyManager);
    	account.$on("altManagerClick", openAltManager);

    	mainbuttons = new MainButtons({
    			props: {
    				$$slots: { default: [create_default_slot_2] },
    				$$scope: { ctx }
    			},
    			$$inline: true
    		});

    	secondarybuttons0 = new SecondaryButtons({
    			props: {
    				$$slots: { default: [create_default_slot_1] },
    				$$scope: { ctx }
    			},
    			$$inline: true
    		});

    	secondarybuttons1 = new SecondaryButtons({
    			props: {
    				$$slots: { default: [create_default_slot] },
    				$$scope: { ctx }
    			},
    			$$inline: true
    		});

    	const block = {
    		c: function create() {
    			main = element("main");
    			div2 = element("div");
    			create_component(logo.$$.fragment);
    			t0 = space();
    			create_component(account.$$.fragment);
    			t1 = space();
    			create_component(mainbuttons.$$.fragment);
    			t2 = space();
    			div0 = element("div");
    			create_component(secondarybuttons0.$$.fragment);
    			t3 = space();
    			div1 = element("div");
    			create_component(secondarybuttons1.$$.fragment);
    			attr_dev(div0, "class", "secondary-buttons left svelte-kubs96");
    			add_location(div0, file, 48, 8, 1743);
    			attr_dev(div1, "class", "secondary-buttons right svelte-kubs96");
    			add_location(div1, file, 55, 8, 2035);
    			attr_dev(div2, "class", "wrapper svelte-kubs96");
    			add_location(div2, file, 36, 4, 1066);
    			attr_dev(main, "class", "svelte-kubs96");
    			add_location(main, file, 35, 0, 1055);
    		},
    		l: function claim(nodes) {
    			throw new Error("options.hydrate only works if the component was compiled with the `hydratable: true` option");
    		},
    		m: function mount(target, anchor) {
    			insert_dev(target, main, anchor);
    			append_dev(main, div2);
    			mount_component(logo, div2, null);
    			append_dev(div2, t0);
    			mount_component(account, div2, null);
    			append_dev(div2, t1);
    			mount_component(mainbuttons, div2, null);
    			append_dev(div2, t2);
    			append_dev(div2, div0);
    			mount_component(secondarybuttons0, div0, null);
    			append_dev(div2, t3);
    			append_dev(div2, div1);
    			mount_component(secondarybuttons1, div1, null);
    			current = true;
    		},
    		p: function update(ctx, [dirty]) {
    			const mainbuttons_changes = {};

    			if (dirty & /*$$scope*/ 2) {
    				mainbuttons_changes.$$scope = { dirty, ctx };
    			}

    			mainbuttons.$set(mainbuttons_changes);
    			const secondarybuttons0_changes = {};

    			if (dirty & /*$$scope*/ 2) {
    				secondarybuttons0_changes.$$scope = { dirty, ctx };
    			}

    			secondarybuttons0.$set(secondarybuttons0_changes);
    			const secondarybuttons1_changes = {};

    			if (dirty & /*$$scope*/ 2) {
    				secondarybuttons1_changes.$$scope = { dirty, ctx };
    			}

    			secondarybuttons1.$set(secondarybuttons1_changes);
    		},
    		i: function intro(local) {
    			if (current) return;
    			transition_in(logo.$$.fragment, local);
    			transition_in(account.$$.fragment, local);
    			transition_in(mainbuttons.$$.fragment, local);
    			transition_in(secondarybuttons0.$$.fragment, local);
    			transition_in(secondarybuttons1.$$.fragment, local);
    			current = true;
    		},
    		o: function outro(local) {
    			transition_out(logo.$$.fragment, local);
    			transition_out(account.$$.fragment, local);
    			transition_out(mainbuttons.$$.fragment, local);
    			transition_out(secondarybuttons0.$$.fragment, local);
    			transition_out(secondarybuttons1.$$.fragment, local);
    			current = false;
    		},
    		d: function destroy(detaching) {
    			if (detaching) detach_dev(main);
    			destroy_component(logo);
    			destroy_component(account);
    			destroy_component(mainbuttons);
    			destroy_component(secondarybuttons0);
    			destroy_component(secondarybuttons1);
    		}
    	};

    	dispatch_dev("SvelteRegisterBlock", {
    		block,
    		id: create_fragment.name,
    		type: "component",
    		source: "",
    		ctx
    	});

    	return block;
    }

    function openProxyManager() {
    	ui.open("proxymanager", screen);
    }

    function openAltManager() {
    	ui.open("altmanager", screen);
    }

    function openSingleplayer() {
    	ui.open("singleplayer", screen);
    }

    function openMultiplayer() {
    	ui.open("multiplayer", screen);
    }

    function openOptions() {
    	ui.open("options", screen);
    }

    function scheduleStop() {
    	minecraft.scheduleStop();
    }

    function instance($$self, $$props, $$invalidate) {
    	let { $$slots: slots = {}, $$scope } = $$props;
    	validate_slots("MainMenu", slots, []);
    	const writable_props = [];

    	Object.keys($$props).forEach(key => {
    		if (!~writable_props.indexOf(key) && key.slice(0, 2) !== "$$") console.warn(`<MainMenu> was created with unknown prop '${key}'`);
    	});

    	$$self.$capture_state = () => ({
    		Account,
    		Logo,
    		ChildButton,
    		MainButton,
    		MainButtons,
    		IconButton,
    		IconTextButton,
    		SecondaryButtons,
    		openProxyManager,
    		openAltManager,
    		openSingleplayer,
    		openMultiplayer,
    		openOptions,
    		scheduleStop
    	});

    	return [];
    }

    class MainMenu extends SvelteComponentDev {
    	constructor(options) {
    		super(options);
    		init(this, options, instance, create_fragment, safe_not_equal, {});

    		dispatch_dev("SvelteRegisterComponent", {
    			component: this,
    			tagName: "MainMenu",
    			options,
    			id: create_fragment.name
    		});
    	}
    }

    const app = new MainMenu({
    	target: document.body,
    	props: {}
    });

    return app;

}());
//# sourceMappingURL=bundle.js.map
