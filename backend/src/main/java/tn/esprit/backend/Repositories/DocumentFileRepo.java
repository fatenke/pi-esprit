package tn.esprit.backend.Repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.esprit.backend.Entities.DocumentFile;

import java.util.List;

public interface DocumentFileRepo extends MongoRepository<DocumentFile, String> {
    List<DocumentFile> findByRoomId(String roomId);
}
