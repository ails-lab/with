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
dbx.Annotation.find({"score":{"$exists":1}}).forEach( function(record) {
  if( record.score.approvedBy instanceof Array ) {
    convertArray( record.score.approvedBy );
   }
  if( record.score.rejectedBy instanceof Array ) {
    convertArray( record.score.rejectedBy );
   }
  if( record.score.dontKnowBy instanceof Array ) {
    convertArray( record.score.dontKnowBy );
   }
  print( record );
  // dbx.Annotation.save( record );
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

	//copy missing records
	sourceDb[collection].find().forEach( function( rec) {
		if( missing[ rec._id ] !== undefined ) {
			targetDb[collection].insert( rec );
			print( rec );
		}
	} );
}

missCopy( "User", dbx, db );
missCopy( "UserGroup", dbx, db );
missCopy( "CollectionObject", dbx, db );
missCopy( "Annotation", dbx, db );
missCopy( "RecordResource", dbx, db );


// fix up stuff that is modified inside the objects
// CollectionObject do they have the same collectedResources?
var collectedRecords = {};
dbx.CollectionObject.find( ).forEach( function (rec) {
	collectedRecords[rec._id] = [];
	for( var idx in rec.collectedResources ) {
		collectedRecords[rec._id].push( rec.collectedResources[idx].target.recordId );
	}
});
// check the results
db.CollectionObject.find().forEach( function (rec ) {
	for( var idx in rec.collectedResources ) {
	
}) 
for( var key in Object.keySet( collectedRecords ))
	