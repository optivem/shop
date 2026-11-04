package com.mycompany.myshop.backend.core.services;

import com.mycompany.myshop.backend.core.exceptions.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class PageSpecTest {

    @Test
    void missingPageAndSizeAreTheFirstPageAtTheDefaultSize() {
        assertThat(PageSpec.of(null, null)).isEqualTo(new PageSpec(1, 50));
    }

    @Test
    void sizeOfOneHundredIsAllowed() {
        assertThat(PageSpec.of(2, 100)).isEqualTo(new PageSpec(2, 100));
    }

    @Test
    void sizeAboveTheCapIsRejected() {
        var thrown = catchThrowableOfType(ValidationException.class, () -> PageSpec.of(null, 101));

        assertThat(thrown.getFieldName()).isEqualTo("size");
        assertThat(thrown).hasMessage("Page size must be between 1 and 100");
    }

    @Test
    void sizeBelowOneIsRejected() {
        var thrown = catchThrowableOfType(ValidationException.class, () -> PageSpec.of(null, 0));

        assertThat(thrown.getFieldName()).isEqualTo("size");
    }

    @Test
    void pageBelowOneIsRejected() {
        var thrown = catchThrowableOfType(ValidationException.class, () -> PageSpec.of(0, null));

        assertThat(thrown.getFieldName()).isEqualTo("page");
        assertThat(thrown).hasMessage("Page must be 1 or greater");
    }

    @Test
    void offsetOfAHugePageDoesNotOverflow() {
        var spec = PageSpec.of(2_000_000_000, 100);

        assertThat(spec.offset()).isEqualTo(199_999_999_900L);
        assertThat(spec.reaches(1_000)).isFalse();
    }

    @Test
    void emptyListHasZeroPages() {
        assertThat(PageSpec.of(null, null).totalPages(0)).isZero();
        assertThat(PageSpec.of(null, 10).totalPages(26)).isEqualTo(3);
    }
}
