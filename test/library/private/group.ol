define group
{
  with( groupRequest ){
    .data << db.bios;
    with( .query ){
      .aggregate << {
        .dstPath = "name.first",
        .srcPath = "name.second"
      };
      .groupBy << {
        .dstPath = "name.first",
        .srcPath = "name.second"
      }
    }
  }
}