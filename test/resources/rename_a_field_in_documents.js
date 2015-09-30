db.User.update(
        {},
        { $rename: { "photo": "thumbnail" } },
        { multi: true }
);

