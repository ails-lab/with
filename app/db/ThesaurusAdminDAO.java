package db;

import model.resources.ThesaurusAdmin;
import model.resources.ThesaurusObject;
import org.bson.types.ObjectId;
import search.Query;

import java.util.List;
import java.util.stream.Collectors;

public class ThesaurusAdminDAO extends DAO<ThesaurusAdmin> {

    public ThesaurusAdminDAO() {
        super(ThesaurusAdmin.class);
    }

    public List<ThesaurusAdmin> getUserAccessibleThesaurusAdminObjects(ObjectId id) {
//        Query<ThesaurusAdmin> q = this.createQuery().disableValidation()
//                                    .field("access.isPublic").equal(true)

        List<ThesaurusAdmin> all = this.findAll().collect(Collectors.toList());
        for (ThesaurusAdmin adm : all) {
            if (adm.getAccess().getIsPublic()) {
                continue;
            }
            if (!adm.getAccess().canRead(id)) {
                all.remove(adm);
            }
        }
        return all;
    }




}
