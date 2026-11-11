package com.eyram.dev.church_project_spring.service.tenant;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import com.eyram.dev.church_project_spring.utils.exception.ParishStorageUnavailableException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ParishStorageDirectoryTest {
    @Test void unavailablePlacementIsRetryableAndNeverFallsBackToAnotherDatabase() {
        var jdbc=mock(JdbcTemplate.class);var directory=new ParishStorageDirectory(jdbc);var parish=UUID.randomUUID();
        when(jdbc.queryForList(anyString(),eq(String.class),eq(parish))).thenReturn(List.of(),List.of("other"));
        for(int attempt=0;attempt<2;attempt++) {
            var failure=catchThrowableOfType(()->directory.requireSharedStorage(parish),ParishStorageUnavailableException.class);
            assertThat(failure).isNotNull();
        }
    }
    @Test void sharedParishIsAcceptedAndMissingParishNeverBecomesGlobal() {
        var jdbc=mock(JdbcTemplate.class);var directory=new ParishStorageDirectory(jdbc);var parish=UUID.randomUUID();
        when(jdbc.queryForList(anyString(),eq(String.class),eq(parish))).thenReturn(List.of("primary"));
        assertThatCode(()->directory.requireSharedStorage(parish)).doesNotThrowAnyException();
        assertThatThrownBy(()->directory.requireSharedStorage(null)).isInstanceOf(ParishStorageUnavailableException.class);
        verify(jdbc,times(1)).queryForList(anyString(),eq(String.class),eq(parish));
    }
}
