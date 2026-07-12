package com.skyinit.pomodorotimer.data.model;

/**
 * 表单字段校验错误，供 View 层定位到具体输入框。
 */
public final class FormFieldError {

    public static final int FIELD_USER_ID = 1;
    public static final int FIELD_NICKNAME = 2;
    public static final int FIELD_PASSWORD = 3;
    public static final int FIELD_CONFIRM_PASSWORD = 4;
    public static final int FIELD_OLD_PASSWORD = 5;
    public static final int FIELD_NEW_PASSWORD = 6;
    public static final int FIELD_TIP = 7;

    public final int field;
    public final String message;

    public FormFieldError(int field, String message) {
        this.field = field;
        this.message = message;
    }
}
