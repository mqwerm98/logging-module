package log.munzi.service;

import log.munzi.entity.Munzi;
import log.munzi.repository.MunziRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
@Service
@RequiredArgsConstructor
public class MunziService {

    private final MunziRepository munziRepository;


    public String getNames() {
        StringBuilder result = new StringBuilder();
        List<Munzi> list = munziRepository.findAll();
        if (list.size() > 0) {
            list.forEach(m -> result.append(m.getName()).append(", "));
        }

        return result.toString();
    }

    public void createMunzi(String name) {
        Munzi munzi = new Munzi(name);
        munziRepository.save(munzi);
    }
}
