package cn.ninanina.wushan.web.result;

public enum ResultMsg {

    SUCCESS("000000", "操作成功"),
    FAILED("999999", "操作失败"),
    ParamError("000001", "参数错误"),

    INVALID_VIDEO_ID("000002", "无效视频id"),
    NOT_LOGIN("000003", "未登录"),
    USER_EXIST("000011", "用户名已存在"),
    EMPTY_CONTENT("000004", "空内容错误"),
    COLLECT_WRONG_DIR("000005", "错误文件夹"),
    COLLECT_ALREADY("000010", "已收藏过了"),
    COLLECT_CANCEL("000007", "取消收藏成功"),

    SECRET_INVALID("000008", "secret值无效"),
    APPKEY_INVALID("000009", "appKey无效"),
    VIDEO_INVALID("000012", "视频失效"),
    BROWSER_INVALID("000013", "浏览器失效"),
    INVALID_COMMENT_ID("000014", "无效评论id"),
    INVALID_TAG_ID("000015", "无效标签id");

    ResultMsg(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    private final String code;
    private final String msg;

    public String getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
