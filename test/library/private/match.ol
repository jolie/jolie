define match
{
  /* Testing ergonomics of binary and unary exp */
  with( matchRequest ){
    .data << bios;
    with( .query.and ) {
      .left.exists = "awards";
      with( .right.or ) {
        with( .left ) {
          .equal << {
            .path = "name.first",
            .value = "Kristen"
          }
        };
        with( .right ) {
          .equal << {
            .path = "contributions",
            .value = "Simula"
          }
        }
      }
    }
  }
}

define match_bool
{
  /* Testing corner stone expression */
  matchRequest.data << bios;
  matchRequest.query = false
}

define match_bool_bis
{
  /* Testing corner stone expression */
  matchRequest.data << bios;
  matchRequest.query.not = true
}

define match_equal_value
{
  /* Testing structural equality with array of trees */
  with( matchRequest ){
    .data << bios;
    tree_awards[0] << matchRequest.data[4].awards;
    tree_awards[1] << matchRequest.data[6].awards;
    with( .query.equal ){
      .path = "awards";
      .value << tree_awards
    }
  }
}

define match_equal_value_bis
{
  /* Testing structural equality just with array of roots */
  with( matchRequest ){
    .data << bios;
    with( .query.equal ){
      .path = "contributions";
      .value[0] = "Simula";
      .value[1] = "OOP";
      .value[2] = "Other"
    }
  }
}