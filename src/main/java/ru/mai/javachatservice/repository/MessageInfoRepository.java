package ru.mai.javachatservice.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.mai.javachatservice.model.client.MessageInfo;

@Repository
public interface MessageInfoRepository extends CrudRepository<MessageInfo, String> {

}
