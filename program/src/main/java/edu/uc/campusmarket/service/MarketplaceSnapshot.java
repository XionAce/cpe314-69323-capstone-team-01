// The snapshot is the data bundle transferred from background loading to the JavaFX interface.
package edu.uc.campusmarket.service;

// These imported model types describe the records in each list; Path describes a file location.
import edu.uc.campusmarket.model.Category;
import edu.uc.campusmarket.model.Listing;
import edu.uc.campusmarket.model.Member;
import edu.uc.campusmarket.model.Seller;

import java.nio.file.Path;
import java.util.List;

/** Immutable data passed from the background worker to the JavaFX controller. */
// A record creates final fields and accessors such as members() and databasePath().
// It carries data only; it does not run SQL or create controls.
public record MarketplaceSnapshot(List<Member> members, List<Seller> sellers,
                                  List<Category> categories, List<Listing> listings,
                                  Path databasePath) {
    // The compact constructor copies collection parameters before record fields are assigned.
    public MarketplaceSnapshot {
        // final alone would not protect a mutable List; List.copyOf prevents later list edits.
        members = List.copyOf(members);
        // Each snapshot keeps its own unmodifiable list, rather than a caller's mutable list.
        sellers = List.copyOf(sellers);
        categories = List.copyOf(categories);
        listings = List.copyOf(listings);
    }
}
