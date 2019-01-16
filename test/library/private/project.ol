/*

*/

define resetQuery {
  println@Console( "\n\n" )();
  undef( projectRequest.query );
  query -> projectRequest.query[#projectRequest.query]
}

define project_path
{
  println@Console( "= = = PROJECT PATH = = = " )();
  query = "name"
}

define project_value
{
  println@Console( "= = = PROJECT VALUE = = = " )();
  query << { .dstPath = "contributions", .value[0] = "C1", .value[1] = "C2", .value[2] = "C3" }
}

define project_value_path
{
  println@Console( "= = = PROJECT VALUE PATH = = = " )();
  query << { .dstPath = "contributions", .value.path = "awards" }
}

define project_value_match
{
  println@Console( "= = = PROJECT VALUE MATCH = = = " )();
  query << { .dstPath = "testValueMatch_isKristen", 
    .value.match.equal.path = "name.first", 
    .value.match.equal.value = "Kristen" 
  }
}

define project_value_ternary
{
  println@Console( "= = = PROJECT VALUE TERNARY = = = " )();
  query << {
    .dstPath = "awards",
    .value.ternary.condition.and.left.equal.path = "name.first",
    .value.ternary.condition.and.right.equal.path = "name.last",
    .value.ternary.condition.and.left.equal.value = "Kristen",
    .value.ternary.condition.and.right.equal.value = "Nyygard",
    .value.ternary.ifTrue.path = "awards",
    .value.ternary.ifFalse = "No Awards if it is not Kristen Nyygard"
  }
}

define failing_project_value_chain {
  println@Console( "= = = FAILING PROJECT VALUE CHAIN = = = " )();
  project_path;
  project_value;
  project_value_path;
  project_value_match;
  project_value_ternary
}

define successful_project_value_chain {
  println@Console( "= = = SUCCESSFUL PROJECT VALUE CHAIN = = = " )();
  project_path;
  project_value_path;
  project_value_match;
  project_value_ternary
}
