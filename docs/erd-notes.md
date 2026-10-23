# Preliminary ERD — proposal model

This diagram is a planning reference. It does not claim approval, implemented tables, migrations, or a working application.

`erd.png` is the readable image; `erd.svg` is the editable vector source. Both show the same entities, attributes, and relationships.

- Each foreign key points to the corresponding primary key named in its field label. Each parent can have zero or more related rows.
- `Category.parentId` is optional for a root category. A nested category has at most one parent; nesting has no fixed number of levels. Cycle prevention remains an implementation requirement.
- A Member can be referenced by Seller, Review (`reviewerId`), and BuyerInterest (`buyerId`). The proposal does not yet restrict one seller profile per member.
- Seller and Category each relate to many Listings. Listing relates to many Reviews and BuyerInterest entries. Category also relates to many HistoricalSale records.
- One matching subtype-details record per Listing is proposed for Textbook, Electronics, or Furniture. Detail tables and fields are not finalized and are intentionally omitted.
- Attribute names reproduce the proposal. SQL types, required-field constraints, uniqueness, deletion policies, and validation rules still need to be finalized. No extra columns are implied.

Entity attributes: Member(id, name); Seller(id, memberId, displayName, sellerKind); Category(id, parentId, name); Listing(id, sellerId, categoryId, type, title, priceMinor, currency, status); Review(id, listingId, reviewerId, rating, comment); BuyerInterest(id, listingId, buyerId, joiningOrder); HistoricalSale(id, categoryId, sourceRecordId, priceMinor, currency, soldAt).
