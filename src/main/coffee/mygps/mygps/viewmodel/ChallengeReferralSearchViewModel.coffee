#
# Licensed to Apereo under one or more contributor license
# agreements. See the NOTICE file distributed with this work
# for additional information regarding copyright ownership.
# Apereo licenses this file to you under the Apache License,
# Version 2.0 (the "License"); you may not use this file
# except in compliance with the License.  You may obtain a
# copy of the License at the following location:
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#

namespace 'mygps.viewmodel'

	ChallengeReferralSearchViewModel:
		
		class ChallengeReferralSearchViewModel extends mygps.viewmodel.AbstractTasksViewModel
			
			constructor: ( session, taskService, challengeService, challengeReferralService ) ->
				super( session, taskService )
				@challengeService = challengeService
				@challengeReferralService = challengeReferralService
				
				@query = ko.observable( null )
				@challenges = ko.observableArray()
				@selectedChallenge = ko.observable( null )
				@selectedChallengeName = ko.dependentObservable( @evaluateSelectedChallengeName, this )
				@referrals = ko.dependentObservable( @evaluateReferrals, this )
				@filteredReferrals = ko.dependentObservable( @filterReferrals, this )
				@allowCustomTaskCreation = ko.observable( false )
			
			evaluateReferrals: () ->
				return @selectedChallenge()?.challengeReferrals() or []
			
			evaluateSelectedChallengeName: () ->
				return @selectedChallenge()?.name()
			
			filterReferrals: () ->
				return _.select(
					@referrals(),
					( referral ) =>
						return not _.any( 
							@tasks(), 
							( task ) ->
								return task.challengeReferralId() is referral.id() and not task.completed()
						)
				)
			
			load: ( query = "") ->
				super()
				@search( query )
				return
			
			createTaskForChallengeReferral: ( challenge, challengeReferral ) ->
				super(
					challenge, 
					challengeReferral, 
					result: ( result ) =>
						@refresh()
					fault: ( fault ) =>
						alert( fault.responseText )
				)				
				
				return
			
			markTask: ( task, complete ) ->
				super( task, complete )
				@refresh()
				return
				
			deleteTask: ( task ) ->
				super(task,
					result: ( result ) =>
						@refresh()
					fault: ( fault ) =>
						alert( fault.responseText )
				)
				return
			
			search: ( query ) ->
				@query( query )
				@reset()
				if ( query? )
					@searchChallenges( query )
				return
			
			refresh: () ->
				#if ( @query()? )
					@searchChallenges( @query() )
					
			reset: () ->
				@challenges.removeAll()
				@selectedChallenge( null )
				
			selectChallenge: ( challenge ) ->
				@selectedChallenge( challenge )
				if ( @selectedChallenge()? )
					if ( @selectedChallenge().referralCount() > 0 and @selectedChallenge().challengeReferrals().length == 0 )
						@searchChallengeReferrals( @selectedChallenge(), @query() )
				
			searchChallenges: ( query ) ->
				@challengeService.search(
					query,
					result: ( result ) =>
						@challenges( result )
						@allowCustomTaskCreation( @challenges().length is 0 )
					fault: ( fault ) =>
						alert( fault.responseText )
				)
				return
				
			searchChallengeReferrals: ( challenge, query ) ->
				@challengeReferralService.search(
					challenge.id(),
					query,
					result: ( result ) =>
						challenge.challengeReferrals( result )
					fault: ( fault ) =>
						alert( fault.responseText )
				)
