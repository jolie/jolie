/*

*/

define project_path
{
  with( projectRequest ){
    .data << bios;
    .query = "contributions"
  }
}

define project_value
{
  with( projectRequest ){
    .data << bios;
    with( .query ){
      .dstPath = "contributions";
      .value = true
    }
  }
}

define project_value_path
{
  with( projectRequest ){
    .data << bios;
    with( .query ){
      .dstPath = "contributions";
      .value = "awards"
    }
  }
}

define project_value_match
{
  nullProcess
}