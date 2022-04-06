/**
 * MIT License
 *
 * Copyright (c) 2017-2022 Julb
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package me.julb.applications.github.actions;

import java.util.Locale;
import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * The label imported from specified source. <br>
 * @author Julb.
 */
@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
@Getter
public class LabelDTO implements Comparable<LabelDTO> {

    // @formatter:off
    /**
     * The name attribute.
     * -- GETTER --
     * Getter for {@link #name} property.
     * @return the value.
     * -- SETTER --
     * Setter for {@link #name} property.
     * @param name the value to set.
     */
    // @formatter:on
    @NonNull
    private String name;

    // @formatter:off
    /**
     * The color attribute.
     * -- GETTER --
     * Getter for {@link #color} property.
     * @return the value.
     * -- SETTER --
     * Setter for {@link #color} property.
     * @param name the value to set.
     */
    // @formatter:on
    @NonNull
    private String color;

    // @formatter:off
    /**
     * The description attribute.
     * -- GETTER --
     * Getter for {@link #description} property.
     * @return the value.
     * -- SETTER --
     * Setter for {@link #description} property.
     * @param name the value to set.
     */
    // @formatter:on
    private String description;

    // ------------------------------------------ Utility methods.

    /**
     * A method to use the lower-cased name for {@link Object#equals(Object)} and {@link Object#hashCode()}.
     * @return the name in lowercase.
     */
    String nameLowerCase() {
        return name.toLowerCase(Locale.ROOT);
    }

    // ------------------------------------------ Read methods.

    // ------------------------------------------ Write methods.

    // ------------------------------------------ Overridden methods.

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        LabelDTO other = (LabelDTO) obj;
        return Objects.equals(nameLowerCase(), other.nameLowerCase());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(nameLowerCase());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(LabelDTO o) {
        return nameLowerCase().compareTo(o.nameLowerCase());
    }
}
