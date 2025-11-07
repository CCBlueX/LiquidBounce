/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

package net.ccbluex.liquidbounce.utils.render;

import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.navigation.NavigationAxis;
import net.minecraft.client.gui.navigation.NavigationDirection;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;

/**
 * Float version of {@link ScreenRect}
 *
 * @param position
 * @param width
 * @param height
 */
public record ScreenRectF(ScreenPosF position, float width, float height) {
    private static final ScreenRectF EMPTY = new ScreenRectF(0, 0, 0, 0);

    public ScreenRectF(float sameAxis, float otherAxis, float width, float height) {
        this(new ScreenPosF(sameAxis, otherAxis), width, height);
    }

    public static ScreenRectF empty() {
        return EMPTY;
    }

    public static ScreenRectF of(NavigationAxis axis, float sameAxisCoord, float otherAxisCoord, float sameAxisLength, float otherAxisLength) {
        ScreenRectF var10000;
        switch (axis) {
            case HORIZONTAL ->
                var10000 = new ScreenRectF(sameAxisCoord, otherAxisCoord, sameAxisLength, otherAxisLength);
            case VERTICAL -> var10000 = new ScreenRectF(otherAxisCoord, sameAxisCoord, otherAxisLength, sameAxisLength);
            default -> throw new MatchException((String) null, (Throwable) null);
        }

        return var10000;
    }

    public ScreenRectF add(NavigationDirection direction) {
        return new ScreenRectF(this.position.add(direction), this.width, this.height);
    }

    public float getLength(NavigationAxis axis) {
        float var10000;
        switch (axis) {
            case HORIZONTAL -> var10000 = this.width;
            case VERTICAL -> var10000 = this.height;
            default -> throw new MatchException((String) null, (Throwable) null);
        }

        return var10000;
    }

    public float getBoundingCoordinate(NavigationDirection direction) {
        NavigationAxis navigationAxis = direction.getAxis();
        return direction.isPositive() ? this.position.getComponent(navigationAxis) + this.getLength(navigationAxis) - 1 : this.position.getComponent(navigationAxis);
    }

    public ScreenRectF getBorder(NavigationDirection direction) {
        float i = this.getBoundingCoordinate(direction);
        NavigationAxis navigationAxis = direction.getAxis().getOther();
        float j = this.getBoundingCoordinate(navigationAxis.getNegativeDirection());
        float k = this.getLength(navigationAxis);
        return of(direction.getAxis(), i, j, 1, k).add(direction);
    }

    public boolean overlaps(ScreenRectF other) {
        return this.overlaps(other, NavigationAxis.HORIZONTAL) && this.overlaps(other, NavigationAxis.VERTICAL);
    }

    public boolean overlaps(ScreenRectF other, NavigationAxis axis) {
        float i = this.getBoundingCoordinate(axis.getNegativeDirection());
        float j = other.getBoundingCoordinate(axis.getNegativeDirection());
        float k = this.getBoundingCoordinate(axis.getPositiveDirection());
        float l = other.getBoundingCoordinate(axis.getPositiveDirection());
        return Math.max(i, j) <= Math.min(k, l);
    }

    public float getCenter(NavigationAxis axis) {
        return (this.getBoundingCoordinate(axis.getPositiveDirection()) + this.getBoundingCoordinate(axis.getNegativeDirection())) / 2;
    }

    @Nullable
    public ScreenRectF intersection(ScreenRectF other) {
        float i = Math.max(this.getLeft(), other.getLeft());
        float j = Math.max(this.getTop(), other.getTop());
        float k = Math.min(this.getRight(), other.getRight());
        float l = Math.min(this.getBottom(), other.getBottom());
        return i < k && j < l ? new ScreenRectF(i, j, k - i, l - j) : null;
    }

    @Nullable
    public ScreenRectF intersection(ScreenRect other) {
        float i = Math.max(this.getLeft(), other.getLeft());
        float j = Math.max(this.getTop(), other.getTop());
        float k = Math.min(this.getRight(), other.getRight());
        float l = Math.min(this.getBottom(), other.getBottom());
        return i < k && j < l ? new ScreenRectF(i, j, k - i, l - j) : null;
    }

    public boolean intersects(ScreenRectF other) {
        return this.getLeft() < other.getRight() && this.getRight() > other.getLeft() && this.getTop() < other.getBottom() && this.getBottom() > other.getTop();
    }

    public boolean contains(ScreenRectF other) {
        return other.getLeft() >= this.getLeft() && other.getTop() >= this.getTop() && other.getRight() <= this.getRight() && other.getBottom() <= this.getBottom();
    }

    public float getTop() {
        return this.position.y();
    }

    public float getBottom() {
        return this.position.y() + this.height;
    }

    public float getLeft() {
        return this.position.x();
    }

    public float getRight() {
        return this.position.x() + this.width;
    }

    public boolean contains(float x, float y) {
        return x >= this.getLeft() && x < this.getRight() && y >= this.getTop() && y < this.getBottom();
    }

    public ScreenRectF transform(Matrix3x2f transformation) {
        Vector2f vector2f = transformation.transformPosition((float) this.getLeft(), (float) this.getTop(), new Vector2f());
        Vector2f vector2f2 = transformation.transformPosition((float) this.getRight(), (float) this.getBottom(), new Vector2f());
        return new ScreenRectF(MathHelper.floor(vector2f.x), MathHelper.floor(vector2f.y), MathHelper.floor(vector2f2.x - vector2f.x), MathHelper.floor(vector2f2.y - vector2f.y));
    }

    public ScreenRectF transformEachVertex(Matrix3x2f transformation) {
        Vector2f vector2f = transformation.transformPosition((float) this.getLeft(), (float) this.getTop(), new Vector2f());
        Vector2f vector2f2 = transformation.transformPosition((float) this.getRight(), (float) this.getTop(), new Vector2f());
        Vector2f vector2f3 = transformation.transformPosition((float) this.getLeft(), (float) this.getBottom(), new Vector2f());
        Vector2f vector2f4 = transformation.transformPosition((float) this.getRight(), (float) this.getBottom(), new Vector2f());
        float f = Math.min(Math.min(vector2f.x(), vector2f3.x()), Math.min(vector2f2.x(), vector2f4.x()));
        float g = Math.max(Math.max(vector2f.x(), vector2f3.x()), Math.max(vector2f2.x(), vector2f4.x()));
        float h = Math.min(Math.min(vector2f.y(), vector2f3.y()), Math.min(vector2f2.y(), vector2f4.y()));
        float i = Math.max(Math.max(vector2f.y(), vector2f3.y()), Math.max(vector2f2.y(), vector2f4.y()));
        return new ScreenRectF(MathHelper.floor(f), MathHelper.floor(h), MathHelper.ceil(g - f), MathHelper.ceil(i - h));
    }

    // Custom

    /**
     * Shrinks the rectangle to the nearest integer coordinates.
     */
    public ScreenRect shrinkToInt() {
        return new ScreenRect(MathHelper.floor(this.getLeft()), MathHelper.floor(this.getTop()), MathHelper.ceil(this.width()), MathHelper.ceil(this.height()));
    }

    /**
     * Expands the rectangle to the nearest integer coordinates.
     */
    public ScreenRect expandToInt() {
        return new ScreenRect(MathHelper.floor(this.getLeft()), MathHelper.floor(this.getTop()), MathHelper.ceil(this.width()), MathHelper.ceil(this.height()));
    }
}
