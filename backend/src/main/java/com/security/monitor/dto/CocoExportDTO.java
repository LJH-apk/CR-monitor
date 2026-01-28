package com.security.monitor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CocoExportDTO {
    private List<CocoImage> images;
    private List<CocoAnnotation> annotations;
    private List<CocoCategory> categories;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CocoImage {
        private Long id;
        private String fileName;
        private Integer width;
        private Integer height;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CocoAnnotation {
        private Long id;
        private Long imageId;
        private Long categoryId;
        private List<Double> bbox;  // [x, y, width, height]
        private Double area;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CocoCategory {
        private Long id;
        private String name;
    }
}
