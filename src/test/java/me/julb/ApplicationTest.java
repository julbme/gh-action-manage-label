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

package me.julb;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import me.julb.sdk.github.actions.spi.GitHubActionProvider;
import me.julb.sdk.github.actions.spi.GitHubActionServiceLoader;

/**
 * Test class for {@link Application} class. <br>
 * @author Julb.
 */
class ApplicationTest {

    /**
     * Test method.
     */
    @Test
    void whenExecuteMainWithImplementation_thenExecuteAction() {
        try (MockedStatic<GitHubActionServiceLoader> sl = Mockito.mockStatic(GitHubActionServiceLoader.class)) {
            var ghAction = Mockito.mock(GitHubActionProvider.class);
            sl.when(GitHubActionServiceLoader::getImplementation).thenReturn(Optional.of(ghAction));
            assertDoesNotThrow(() -> Application.main(new String[0]));
            verify(ghAction).execute();
        }
    }

    /**
     * Test method.
     */
    @Test
    void whenExecuteMainWithNoImplementation_thenFail() {
        try (MockedStatic<GitHubActionServiceLoader> sl = Mockito.mockStatic(GitHubActionServiceLoader.class)) {
            sl.when(GitHubActionServiceLoader::getImplementation).thenReturn(Optional.empty());
            assertThrows(NoSuchElementException.class, () -> Application.main(new String[0]));
        }
    }
}
