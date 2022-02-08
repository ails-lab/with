package db;

import model.resources.ThesaurusAdmin;
import model.resources.ThesaurusObject;
import org.bson.types.ObjectId;
import org.mongodb.morphia.query.Query;

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

    public ThesaurusAdmin findThesaurusAdminByName(String name) {
        Query<ThesaurusAdmin> q = this.createQuery().field("name").equal(name);

        ThesaurusAdmin adm = this.findOne(q);
        return adm;
    }

    public ThesaurusAdmin findThesaurusAdminById(ObjectId id) {
        Query<ThesaurusAdmin> q = this.createQuery().field("_id").equal(id);

        ThesaurusAdmin adm = this.findOne(q);
        return adm;
    }

    public void removeThesaurusAdmin(ObjectId id) {
        this.removeById(id);
    }





}
