package ru.mai.javachatservice.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.mai.javachatservice.model.client.CipherInfo;

@Repository
public interface CipherInfoRepository extends CrudRepository<CipherInfo, Long> {

}
