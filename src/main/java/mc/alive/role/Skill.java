package mc.alive.role;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Skill {
    // 技能的唯一标识符，用于确定技能的顺序
    int id() default 0;

    // 技能的名称，用于显示和识别
    String name() default "";

    // 技能使用的最低等级要求
    int minLevel() default 0;
}
