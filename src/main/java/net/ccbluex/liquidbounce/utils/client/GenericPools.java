/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ccbluex.liquidbounce.utils.client;

import net.ccbluex.fastutil.Pool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * This should only be used in render thread!
 */
@SuppressWarnings("rawtypes")
public final class GenericPools {
    private GenericPools() {
    }

    public static final Pool<ArrayList> ARRAY_LIST = Pool.create(ArrayList::new, ArrayList::clear);

    public static final Pool<HashMap> HASH_MAP = Pool.create(HashMap::new, HashMap::clear);

    public static final Pool<HashSet> HASH_SET = Pool.create(HashSet::new, HashSet::clear);
}
