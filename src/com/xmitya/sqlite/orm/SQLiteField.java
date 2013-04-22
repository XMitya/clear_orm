package com.xmitya.sqlite.orm;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.FIELD})
public @interface SQLiteField {

    String columnName();

    boolean id() default false;

    boolean autoGenerate() default false;

    String datePattern() default "";
}
