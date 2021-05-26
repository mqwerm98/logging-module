package log.munzi.repository;


import log.munzi.entity.Munzi;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MunziRepository extends JpaRepository<Munzi, Long> {
}
