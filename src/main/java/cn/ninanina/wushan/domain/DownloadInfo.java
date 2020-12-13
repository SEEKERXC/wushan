package cn.ninanina.wushan.domain;

import lombok.Data;

/**
 * xvideos的下载返回json
 */
@Data
public class DownloadInfo {
    private String LOGGED;
    private String MP4HD_AVAILABLE;
    private String MP4_4K_AVAILABLE;
    private String URL;
    private String URL_LOW;
    private String ERROR;
}
