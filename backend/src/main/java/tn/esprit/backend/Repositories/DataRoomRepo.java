package tn.esprit.backend.Repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.esprit.backend.Entities.DataRoom;

public interface DataRoomRepo extends MongoRepository<DataRoom, String> {
}
