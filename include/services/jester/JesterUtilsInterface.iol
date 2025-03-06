/*

  * Copyright (c) 2019 Claudio Guidi <guidiclaudio@gmail.com>

 *   This program is free software; you can redistribute it and/or modify  
 *   it under the terms of the GNU Library General Public License as       
 *   published by the Free Software Foundation; either version 2 of the    
 *   License, or (at your option) any later version.                       
 *                                                                         
 *   This program is distributed in the hope that it will be useful,       
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         
 *   GNU General Public License for more details.                          
 *                                                                         
 *   You should have received a copy of the GNU Library General Public     
 *   License along with this program; if not, write to the                 
 *   Free Software Foundation, Inc.,                                       
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             
 *                                                                         
 *   For details about the authors of this software, see the AUTHORS file. 
*/

include "types/definition_types.iol"

type AnalyzeTemplateResponse: void {
    .method: string
    .template: string | void
}
type GetParamsListResponse: bool {
    .query*: string
    .path*: string
}

type CheckTypeConsistencyRequest: string {
    .type_map: undefined 
}


type GetActualCurrentTypeRequest: string {
    .type_map: undefined 
}

type CheckBranchChoiceConsistency: void {
    .branch: Type
    .type_map: undefined 
}
        
interface JesterUtilsInterface {
RequestResponse:
    getParamList( string )( GetParamsListResponse ),
    checkTypeConsistency( CheckTypeConsistencyRequest )( bool ) throws DefinitionError,
    checkBranchChoiceConsistency( CheckBranchChoiceConsistency )( bool ) throws DefinitionError,
    getActualCurrentType( GetActualCurrentTypeRequest )( string )
}