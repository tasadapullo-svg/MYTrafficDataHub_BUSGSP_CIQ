package com.mytransitgps.gtfs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mytransitgps.gtfs.service.BusGpsStorageLayout;
import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** 验证长期运行 BUS GPS 与 CIQ 采用并列业务目录。 */
class BusGpsStorageLayoutTest {

    @Test
    void newJsonAndArchiveLayoutMustUseBusGpsDomainDirectory() {
        Path workspace = Path.of("data_download");
        assertEquals(Path.of("data_download", "BusGPS"), BusGpsStorageLayout.jsonRoot(workspace));
        assertEquals(Path.of("data_download", "BusGPS", "20260830"),
                BusGpsStorageLayout.dailyJsonRoot(workspace, "20260830"));
        assertEquals(Path.of("data_download", "BusGPS", "archive"),
                BusGpsStorageLayout.archiveRoot(workspace));
        assertEquals(Path.of("data_download", "BusGPS", "archive", "2026", "08"),
                BusGpsStorageLayout.archiveRoot(workspace, LocalDate.of(2026, 8, 30)));
        assertEquals(Path.of("data_download", "json_data"),
                BusGpsStorageLayout.legacyJsonRoot(workspace));
        assertEquals(Path.of("data_download", "daily_archive"),
                BusGpsStorageLayout.legacyArchiveRoot(workspace));
    }
}
