db.Collection.find().forEach(
        function(myCol) { 
            for(f in myCol.rights) { 
                printjson("User: " + f + " has rights " + myCol.rights[f] ); 
                if(myCol.rights[f] == "OWN") {
                    myCol.rights[f] = NumberInt(3);
                } else if (myCol.rights[f] == "READ") {
                    myCol.rights[f] = NumberInt(1);
                } else if (myCol.rights[f] == "WRITE") {
                    myCol.rights[f] = NumberInt(2);
                } else if (myCol.rights[f] == "NONE") {
                    myCol.rights[f] = NumberInt(0);
                }
                printjson("User: " + f + " has rights " + myCol.rights[f] ); 
            }
            db.Collection.save(myCol);
        }
);

