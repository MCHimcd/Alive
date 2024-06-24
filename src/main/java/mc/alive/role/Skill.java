package mc.alive.role;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Skill {
    //技能顺序
    int id() default 0;
    //名称
    String name() default "";
    //最低等级
    int minLevel() default 0;
}
