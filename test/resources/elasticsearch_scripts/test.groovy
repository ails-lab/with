import org.elasticsearch.common.logging.*;

ESLogger logger = ESLoggerFactory.getLogger('log created');
logger.info(doc['_id'].value);
return doc['_type'].value;
