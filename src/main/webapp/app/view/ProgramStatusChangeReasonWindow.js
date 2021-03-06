/*
 * Licensed to Apereo under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Apereo licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License.  You may obtain a
 * copy of the License at the following location:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
Ext.define('Ssp.view.ProgramStatusChangeReasonWindow', {
	extend: 'Ext.window.Window',
	alias : 'widget.programstatuschangereasonwindow',
    mixins: [ 'Deft.mixin.Injectable',
              'Deft.mixin.Controllable'],
    controller: 'Ssp.controller.ProgramStatusChangeReasonWindowViewController',
    inject: {
    	personLite: 'personLite',
    	store: 'programStatusChangeReasonsActiveUnpagedStore',
    	textStore: 'sspTextStore'
    },
	width: '100%',
	height: '100%',
	config: {
		actionCallbacks: null,
		isBulk: false
	},
    initComponent: function(){
    	var me=this;
    	Ext.applyIf(me,
    			   {
				    modal: true, 
		    		layout: 'anchor',
					title: this.textStore.getValueByCode('ssp.label.program-status-change-reason.title','Please provide a reason the student will no longer be participating:'),
    				items: [
					// tbspacer ensures a little bit of breathing room at the top of the form.
					// not sure why exactly, but if the student name displayfield is hidden
					// the rest of the form gets crammed right up to the top of the dialog
					// with no padding.
					{
						xtype: 'tbspacer',
						height: 5
					},{
                    	xtype: 'displayfield',
                    	fieldLabel: 'Student',
                    	value: me.personLite.get('displayFullName'),
                    	anchor: '95%',
                    	hidden: me.getIsBulk(),
                    	disabled: me.getIsBulk()
                    },{
    			        xtype: 'combobox',
    			        itemId: 'programStatusChangeReasonCombo',
    			        name: 'programStatusChangeReasonId',
    			        fieldLabel: me.textStore.getValueByCode('ssp.label.program-status-change-reason.reason','Reason'),
    			        emptyText: me.textStore.getValueByCode('ssp.empty-text.program-status-change-reason.reason','Select One'),
    			        store: me.store,
    			        valueField: 'id',
    			        displayField: 'name',
    			        editable: false,
    			        allowBlank: false,
    			        forceSelection: true,
    			        anchor: '95%'
    		        }],
    	            dockedItems: [{
    		               xtype: 'toolbar',
    		               dock: 'bottom',
    		               items: [{
		       		                   text: me.textStore.getValueByCode('ssp.label.save-button','Save'),
		       		                   xtype: 'button',
		       		                   action: 'save',
		       		                   itemId: 'saveButton'
		       		               }, '-', {
		       		                   text: me.textStore.getValueByCode('ssp.label.cancel-button','Cancel'),
		       		                   xtype: 'button',
		       		                   action: 'cancel',
		       		                   itemId: 'cancelButton'
		       		               }]
    		           }]
		    	    });
    	
    	return me.callParent(arguments);
    },

	show: function() {
		var me = this;
		if ( me.getController().beforeShow(me) ) {
			return me.callParent(arguments);
		}
		return me;
	}
});
