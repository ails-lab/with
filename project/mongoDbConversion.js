// copy Campaign collection
// connect to crowd db
var dbx = db.getSiblingDB("with-crowd-21032018");
// copy the coampaign objects
dbx.Campaign.find().forEach( function(x){db.Campaign.insert(x)} )


// Annotation.score part became more detailed

var dbx = db.getSiblingDB("with-crowd-21032018");

// its either ObjectIds or objects with “withCreator”
function convertArray( arr ) {
  if( arr.length > 0 ) {
    if( arr[0] instanceof ObjectId ) {
    // arr needs conversion
      for( var idx =0; idx < arr.length; idx+=1 ) {
         arr[idx] = { "withCreator": arr[idx], 
                      "confidence":0.0, 
                      "generator":"Converted from old schema",
                      "generated":new ISODate(),
                      "lastModified":new ISODate()
                    }
      }
    }
  }
}


// convert this Annotation objects to new schema
// there are not many scores and even fewer approvedBy rejectedBy etc.
db.Annotation.find({"score":{"$exists":1}}).forEach( function(record) {
  if( record.score.approvedBy instanceof Array ) {
    convertArray( record.score.approvedBy );
   }
  if( record.score.rejectedBy instanceof Array ) {
    convertArray( record.score.rejectedBy );
   }
  if( record.score.dontKnowBy instanceof Array ) {
    convertArray( record.score.dontKnowBy );
   }
  //printjson( record );
  db.Annotation.save( record );
} );
//
// User copy
//

var dbx = db.getSiblingDB("with-crowd-21032018");

function missCopy( collection, sourceDb, targetDb ) {
	var missing = {};
	sourceDb[collection].find({},{"_id":1}).forEach( function( rec) { missing[rec._id] = 1;});
	// remove existing 
	targetDb[collection].find({},{"_id":1}).forEach( function( rec) { delete missing[rec._id];});
	var  count = 0;
	
	//copy missing records
	sourceDb[collection].find().forEach( function( rec) {
		if( missing[ rec._id ] !== undefined ) {
			targetDb[collection].insert( rec );
			print( rec );
			count += 1 ;
		}
	} );
	return count;
}

print("Copied Users " +  missCopy( "User", dbx, db ));
missCopy( "UserGroup", dbx, db );
missCopy( "CollectionObject", dbx, db );
missCopy( "Annotation", dbx, db );
missCopy( "RecordResource", dbx, db );


// fix up stuff that is modified inside the objects
// CollectionObject do they have the same collectedResources?
var collectedRecords = {};
dbx.CollectionObject.find( ).forEach( function (rec) {
	collectedRecords[rec._id.valueOf()] = {};
	for( var idx in rec.collectedResources ) {
		collectedRecords[rec._id.valueOf()][ rec.collectedResources[idx].target.recordId +"-" + idx] = 1;
	}
});

// check the results
// take out collected+idx from the crowd db and see what is left
db.CollectionObject.find().forEach( function (rec ) {
	if( collectedRecords[rec._id.valueOf()] === undefined ) {
		collectedRecords[rec._id.valueOf()] = {}; 
	}
	var collected = collectedRecords[rec._id.valueOf()];
	for( var idx in rec.collectedResources ) {
		if( collected[ rec.collectedResources[idx].target.recordId +"-"+idx] !== undefined ) {
			// it already exists, can remove it, its the same
			delete collected[ rec.collectedResources[idx].target.recordId +"-"+idx];
		} else {
			collected[ rec.collectedResources[idx].target.recordId +"-"+idx] = 2;
		}
	}
	if( Object.keySet( collected ).length == 0 ) {
		delete collectedRecords[rec._id.valueOf()];
	}
});

// now collectedRecords should contain differences
for( var key in  collectedRecords ) {
	printjson( collectedRecords[key]);
}

// on different collected resources we take the dbx one
// if the values are all 2 .. the target db is bigger and we keep it
// if the values are mixed or 1 we take collectedResource value from dbx to db
for( var key in  collectedRecords ) {
	var changedRecords = collectedRecords[key];
	var hasOnes = false;
	for( var idx in changedRecords ) {
		if( changedRecords[idx] == 1 ) {
			hasOnes = true;
		}
	}
	if( hasOnes ) {
		// copy the shit
		var oldColObj = dbx.CollectionObject.find( {"_id": ObjectId( key )}).next();
		var newColObj = db.CollectionObject.find( {"_id": ObjectId( key )}).next();
		
		newColObj.collectedResources = oldColObj.collectedResources
		printjson( newColObj );
		// and safe it
		// db.CollectionObject.save( newColObj );
	}
}


// syncing RecordResource with CollectionObjects
// need to update administrative.collectedBy[].{level=0..3,user}
// usage.collected:n-length of collected
// collectedIn[]: ObjectId of collections
// annotationIds[]: ObjectId of annotations

// build in mem struct of collectionObject .. records and owner of collection
// CollectionObject.adminstrative.access.acl.{user, level}

var records = {}
// record.collectedIn [ ObjectIds]
// record.acl[] user,level


db.CollectionObject.find().forEach( function ( col ) {
	var aclArr = col.administrative.access.acl;
	for( var idx in col.collectedResources  ) {
		var recId = col.collectedResources[idx].target.recordId;
		if( recId instanceof ObjectId ) {
			recId = recId.valueOf();
		}
		if( records[ recId ] === undefined ) {
			records[recId] = { "collectedIn":[], "acl":[]};
		}
		var record = records[recId];
		for( var aclIdx in aclArr ) {
			delete aclArr[aclIdx]["className"];
			record.acl.push( aclArr[aclIdx]);
		}
		record.collectedIn.push( col["_id"]);
	} 
});

var count =10;
// now do stuff for each record
for( var idx in records ) {
	var recordStats = records[idx];
	var recordIter = db.RecordResource.find( {"_id":ObjectId(idx)});
	if( !recordIter.hasNext()){
		print( "Missing record " + idx );
	} else {
		var record= recordIter.next();
		// print( "Before");
		// printjson( record );
		record.usage.collected = recordStats.collectedIn.length;
		record.collectedIn - recordStats.collectedIn;
		record.administrative.collectedBy = recordStats.acl;
		// print( "After");
		// printjson( record );
		// count -= 1;
		// if( count == 0 ) { break; }
		db.RecordResource.save( record );
	}
}

// syncing AnnotationIds
var recordAnnotation = {};
db.Annotation.find().forEach( function ( ann ) {
	if( recordAnnotation[ann.target.recordId.valueOf()] === undefined ) {
		recordAnnotation[ann.target.recordId.valueOf()] = [];
	}
	recordAnnotation[ann.target.recordId.valueOf()].push( ann["_id"] );
} );

for( var idx in recordAnnotation) {
	var recId = ObjectId( idx );
	var rec = db.RecordResource.findOne( recId );
    if( rec === null ) {
       print( "Missing " + recId );
    } else {
	  rec.annotationIds = recordAnnotation[idx];
	  //print( "Update " + idx + " with " + recordAnnotation[idx]);
	  db.RecordResource.save( rec );
   }
}
