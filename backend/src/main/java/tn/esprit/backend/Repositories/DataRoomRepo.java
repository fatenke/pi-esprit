package tn.esprit.backend.Repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.esprit.backend.Entities.DataRoom;

import java.util.Optional;

public interface DataRoomRepo extends MongoRepository<DataRoom, String> {
    Optional<DataRoom> findByDealId(String dealId);
}
