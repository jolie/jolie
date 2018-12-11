define lookup
{
  with( lookupRequest ){
    .leftData << bios;
    .leftPath = "";
    .rightPath = "";
    .rightData << new_bios;
    .dstPath = ""
  }
}