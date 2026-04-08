from ..test-unit import TestUnitInterface
from file import File

service Main {

	embed File as File

	inputPort TestUnitInput {
		location: "local"
		interfaces: TestUnitInterface
	}

	init {
		readFile@File( { filename = "library/companies.json", format = "json" } )( data )
	}

	define testPathsCompaniesFoundedAfter2014
	{
		undef( result );
		result << paths data.companies[*] where $.company.founded > 2014;

		if ( #result.results != 4 ) {
			throw( TestFailed, "Expected 4 companies founded after 2014, got " + #result.results )
		}
	}

	define testValuesCompanyNames
	{
		undef( result );
		result << values data.companies[*].company where $.founded > 2014;

		if ( #result.results != 4 ) {
			throw( TestFailed, "Expected 4 companies founded after 2014, got " + #result.results )
		};
		// Verify we got company objects with name field
		if ( result.results[0].name != "GreenPulse Energy" ) {
			throw( TestFailed, "Expected GreenPulse Energy, got " + result.results[0].name )
		}
	}

	define testRecursiveFieldProjects
	{
		undef( result );
		// Find all projects with status "testing" using recursive field
		result << paths data..projects[*] where $.status == "testing";

		if ( #result.results != 5 ) {
			throw( TestFailed, "Expected 5 testing projects, got " + #result.results )
		}
	}

	define testArrayWildcardWithDollar
	{
		undef( result );
		// Find companies where any project uses "Python"
		result << paths data.companies[*].company where $..technologies[*] == "Python";

		if ( #result.results != 6 ) {
			throw( TestFailed, "Expected 6 companies using Python, got " + #result.results )
		}
	}

	define testHasOperatorOnDepartments
	{
		undef( result );
		// Find all teams that have projects
		result << paths data..teams[*] where $ has "projects";

		if ( #result.results != 31 ) {
			throw( TestFailed, "Expected 31 teams with projects field, got " + #result.results )
		}
	}

	define testAllCompanies
	{
		undef( result );
		// Get all companies without filter - should be 11
		result << paths data.companies[*].company where $.founded > 0;

		if ( #result.results != 11 ) {
			throw( TestFailed, "Expected 11 total companies, got " + #result.results )
		}
	}

	define testSimpleFoundedFilter
	{
		undef( result );
		// Test simple founded filter on same path
		result << paths data.companies[*].company where $.founded == 2012;

		if ( #result.results != 1 ) {
			throw( TestFailed, "Expected 1 company founded in 2012, got " + #result.results )
		}
	}

	define testSimpleCityFilter
	{
		undef( result );
		// Test simple city filter
		result << paths data.companies[*].company where $.headquarters.city == "Milano";

		if ( #result.results != 1 ) {
			throw( TestFailed, "Expected 1 Milano company, got " + #result.results )
		}
	}

	define testComplexAndCondition
	{
		undef( result );
		// Find companies in Milano or Torino founded after 2010
		result << paths data.companies[*].company where ($.headquarters.city == "Milano" || $.headquarters.city == "Torino") && $.founded > 2010;

		if ( #result.results != 3 ) {
			throw( TestFailed, "Expected 3 companies, got " + #result.results )
		}
	}

	main {
		test()() {
			testPathsCompaniesFoundedAfter2014;
			testValuesCompanyNames;
			testRecursiveFieldProjects;
			testArrayWildcardWithDollar;
			testHasOperatorOnDepartments;
			testAllCompanies;
			testSimpleFoundedFilter;
			testSimpleCityFilter;
			testComplexAndCondition
		}
	}
}
