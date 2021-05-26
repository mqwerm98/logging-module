package log.munzi.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Munzi {

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    public Munzi(String name) {
        this.name = name;
    }
}
