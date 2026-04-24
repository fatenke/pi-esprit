package tn.esprit.backend.Services;

import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.mongodb.client.gridfs.model.GridFSFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class GridFsStorageService {

    private final GridFsTemplate gridFsTemplate;

    /**
     * Stocke un PDF dans GridFS (bucket fs) et retourne une URL relative servie par l’API.
     */
    public String storeInvestorPdf(MultipartFile file, String startupId) throws IOException {
        Document metadata = new Document();
        metadata.append("startupId", startupId);
        metadata.append("kind", "investor-doc");
        if (file.getContentType() != null) {
            metadata.append("_contentType", file.getContentType());
        }

        ObjectId id = gridFsTemplate.store(
                file.getInputStream(),
                file.getOriginalFilename(),
                metadata
        );
        return "/api/invest-request/files/" + id.toHexString();
    }

    public String store(MultipartFile file, String kind, Document metadata) throws IOException {
        Document mergedMetadata = metadata == null ? new Document() : metadata;
        mergedMetadata.append("kind", kind);
        if (file.getContentType() != null && !mergedMetadata.containsKey("_contentType")) {
            mergedMetadata.append("_contentType", file.getContentType());
        }

        ObjectId id = gridFsTemplate.store(
                file.getInputStream(),
                file.getOriginalFilename(),
                mergedMetadata
        );
        return id.toHexString();
    }

    public GridFsResource load(String hexObjectId) {
        GridFSFile file = gridFsTemplate.findOne(
                Query.query(Criteria.where("_id").is(new ObjectId(hexObjectId)))
        );
        if (file == null) {
            return null;
        }
        return gridFsTemplate.getResource(file);
    }
}
