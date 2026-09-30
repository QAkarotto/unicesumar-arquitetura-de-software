package jakarta.persistence; import java.lang.annotation.*;
@Retention(RetentionPolicy.RUNTIME) public @interface OneToMany { CascadeType[] cascade() default {}; FetchType fetch() default FetchType.LAZY; boolean orphanRemoval() default false; }
