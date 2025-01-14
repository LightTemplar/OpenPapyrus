// CmdROrderPrereqActivity.java
// Copyright (c) A.Sobolev 2021, 2022
//
package ru.petroglif.styloq;

import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.TimerTask;

public class CmdROrderPrereqActivity extends SLib.SlActivity {
	public  CommonPrereqModule CPM;
	private JSONArray WharehouseListData;
	private JSONArray QuotKindListData;
	private ViewDescriptionList VdlDocs; // Описание таблицы просмотра существующих заказов
	private ArrayList <Document.EditAction> DocEditActionList;
	private void RefreshCurrentDocStatus()
	{
		if(CPM.TabList != null) {
			ViewPager2 view_pager = (ViewPager2) findViewById(R.id.VIEWPAGER_ORDERPREREQ);
			if(view_pager != null) {
				int tidx = view_pager.getCurrentItem();
				CommonPrereqModule.TabEntry tab_entry = CPM.TabList.get(tidx);
				if(tab_entry != null && tab_entry.TabId == CommonPrereqModule.Tab.tabCurrentOrder) {
					HandleEvent(SLib.EV_SETVIEWDATA, tab_entry.TabView.getView(), null);
				}
			}
		}
	}
	private class RefreshTimerTask extends TimerTask {
		@Override public void run() { runOnUiThread(new Runnable() { @Override public void run() { RefreshCurrentDocStatus(); }}); }
	}
	public CmdROrderPrereqActivity()
	{
		CPM = new CommonPrereqModule(this);
		DocEditActionList = null;
	}
	private void MakeSimpleSearchIndex()
	{
		CPM.InitSimpleIndex();
		CPM.AddGoodsToSimpleIndex();
		CPM.AddGoodsGroupsToSimpleIndex();
		CPM.AddBrandsToSimpleIndex();
		if(CPM.CliListData != null) {
			for(int i = 0; i < CPM.CliListData.size(); i++) {
				CommonPrereqModule.CliEntry ce = CPM.CliListData.get(i);
				if(ce != null && ce.JsItem != null) {
					int id = ce.JsItem.optInt("id", 0);
					if(id > 0) {
						String nm = ce.JsItem.optString("nm", null);
						CPM.AddSimpleIndexEntry(SLib.PPOBJ_PERSON, id, SLib.PPOBJATTR_NAME, nm, nm);
						String ruinn = ce.JsItem.optString("ruinn");
						CPM.AddSimpleIndexEntry(SLib.PPOBJ_PERSON, id, SLib.PPOBJATTR_RUINN, ruinn, nm);
						String rukpp = ce.JsItem.optString("rukpp");
						CPM.AddSimpleIndexEntry(SLib.PPOBJ_PERSON, id, SLib.PPOBJATTR_RUKPP, rukpp, nm);
						JSONArray dlvr_loc_list = ce.JsItem.optJSONArray("dlvrloc_list");
						if(dlvr_loc_list != null) {
							for(int j = 0; j < dlvr_loc_list.length(); j++) {
								JSONObject js_item = dlvr_loc_list.optJSONObject(j);
								if(js_item != null) {
									final int loc_id = js_item.optInt("id", 0);
									if(loc_id > 0) {
										String addr = js_item.optString("addr");
										CPM.AddSimpleIndexEntry(SLib.PPOBJ_LOCATION, loc_id, SLib.PPOBJATTR_RAWADDR, addr, nm);
									}
								}
							}
						}
					}
				}
			}
		}
	}
	static class TransferItemDialog extends SLib.SlDialog {
		CmdROrderPrereqActivity ActivityCtx;
		public TransferItemDialog(Context ctx, Object data)
		{
			super(ctx, R.id.DLG_ORDRTI, data);
			if(ctx instanceof CmdROrderPrereqActivity)
				ActivityCtx = (CmdROrderPrereqActivity)ctx;
			if(data instanceof Document.TransferItem)
				Data = data;
		}
		private void StepQuantity(int direction) // direction < 0 - decrement; > 0 - increment
		{
			boolean do_update = false;
			if(Data != null && Data instanceof Document.TransferItem) {
				Document.TransferItem _data = (Document.TransferItem)Data;
				CommonPrereqModule.WareEntry goods_item = ActivityCtx.CPM.FindGoodsItemByGoodsID(_data.GoodsID);
				double _upp = 0.0;
				double _mult = 0.0;
				double _min = 0.0;
				int    uom_id = 0;
				if(goods_item != null && goods_item.JsItem != null) {
					uom_id = goods_item.JsItem.optInt("uomid", 0);
					_upp = goods_item.JsItem.optDouble("upp", 0.0);
					_mult = goods_item.JsItem.optDouble("ordqtymult", 0.0);
					_min = goods_item.JsItem.optDouble("ordminqty", 0.0);
				}
				String qtty_text = SLib.GetCtrlString(this, R.id.CTL_ORDRTI_QTTY);
				if(_data.Set == null)
					_data.Set = new Document.ValuSet();
				if(SLib.GetLen(qtty_text) > 0)
					_data.Set.Qtty = SLib.strtodouble(qtty_text);
				if(direction > 0) {
					if(_mult > 0.0)
						_data.Set.Qtty += _mult;
					else if(_upp > 0.0)
						_data.Set.Qtty += _upp;
					else
						_data.Set.Qtty += 1.0;
					do_update = true;
				}
				else if(direction < 0) {
					double decrement = 0.0;
					if(_mult > 0.0)
						decrement = _mult;
					else if(_upp > 0.0)
						decrement = _upp;
					else
						decrement = 1.0;
					if(_data.Set.Qtty >= decrement) {
						_data.Set.Qtty -= decrement;
						do_update = true;
					}
				}
				if(do_update) {
					SLib.SetCtrlString(this, R.id.CTL_ORDRTI_QTTY, ActivityCtx.CPM.FormatQtty(_data.Set.Qtty, uom_id, true));
					SLib.SetCtrlString(this, R.id.CTL_ORDRTI_AMOUNT, ActivityCtx.CPM.FormatCurrency(_data.Set.Qtty * _data.Set.Price));
				}
			}
		}
		@Override public Object HandleEvent(int ev, Object srcObj, Object subj)
		{
			Object result = null;
			switch(ev) {
				case SLib.EV_CREATE:
					requestWindowFeature(Window.FEATURE_NO_TITLE);
					setContentView(R.layout.dialog_ordrti);
					SetDTS(Data);
					{
						EditText vqty = SLib.FindEditTextById(this, R.id.CTL_ORDRTI_QTTY);
						if(vqty != null && vqty instanceof TextInputEditText) {
							((TextInputEditText)vqty).addTextChangedListener(new TextWatcher() {
								public void afterTextChanged(Editable s)
								{
								}
								public void beforeTextChanged(CharSequence s, int start, int count, int after)
								{
								}
								public void onTextChanged(CharSequence s, int start, int before, int count)
								{
									String text = s.toString();
									if(Data != null && SLib.GetLen(text) > 0) {
										Document.TransferItem _data = (Document.TransferItem)Data;
										double qtty = Double.parseDouble(text);
										if(qtty < 0.0)
											qtty = 0.0;
										if(_data.Set == null)
											_data.Set = new Document.ValuSet();
										_data.Set.Qtty = qtty;
										View vamt = findViewById(R.id.CTL_ORDRTI_AMOUNT);
										if(vamt != null && vamt instanceof TextView) {
											String amt_text = ActivityCtx.CPM.FormatCurrency(_data.Set.Qtty * _data.Set.Price);
											((TextView)vamt).setText(amt_text);
										}
									}
								}
							});
						}
					}
					break;
				case SLib.EV_COMMAND:
					if(srcObj != null && srcObj instanceof View) {
						final int view_id = ((View)srcObj).getId();
						if(view_id == R.id.STDCTL_OKBUTTON) {
							Object data = GetDTS();
							if(data != null) {
								//Context ctx = getContext();
								//StyloQApp app_ctx = (ctx != null) ? (StyloQApp)ctx.getApplicationContext() : null;
								//if(app_ctx != null)
								ActivityCtx.HandleEvent(SLib.EV_IADATAEDITCOMMIT, this, data);
							}
							this.dismiss();
						}
						else if(view_id == R.id.STDCTL_CANCELBUTTON) {
							this.dismiss();
						}
						else if(view_id == R.id.STDCTL_DELETEBUTTON) {
							if(Data != null && Data instanceof Document.TransferItem) {
								StyloQApp app_ctx = SLib.SlActivity.GetAppCtx(ActivityCtx);
								if(app_ctx != null) {
									Document.TransferItem _data = (Document.TransferItem)Data;
									if(_data != null && _data.Set != null) {
										int uom_id = 0;
										CommonPrereqModule.WareEntry goods_item = ActivityCtx.CPM.FindGoodsItemByGoodsID(_data.GoodsID);
										if(goods_item != null && goods_item.JsItem != null) {
											uom_id = goods_item.JsItem.optInt("uomid", 0);
										}
										_data.Set.Qtty = 0.0;
										SLib.SetCtrlString(this, R.id.CTL_ORDRTI_QTTY, ActivityCtx.CPM.FormatQtty(_data.Set.Qtty, uom_id, true));
										SLib.SetCtrlString(this, R.id.CTL_ORDRTI_AMOUNT, ActivityCtx.CPM.FormatCurrency(_data.Set.Qtty * _data.Set.Price));
									}
									/*
									try {
										StyloQDatabase db = app_ctx.GetDB();
										db.DeleteForeignSvc(((StyloQDatabase.SecStoragePacket)Data).Rec.ID);
										app_ctx.HandleEvent(SLib.EV_IADATADELETECOMMIT, this, Data);
										this.dismiss(); // Close Dialog
									} catch(StyloQException exn) {
										;
									}
									 */
								}
							}
						}
						else if(view_id == R.id.CTL_QTTY_UP) {
							StepQuantity(+1);
						}
						else if(view_id == R.id.CTL_QTTY_DN) {
							StepQuantity(-1);
						}
					}
					break;
			}
			return result;
		}
		boolean SetDTS(Object objData)
		{
			boolean ok = true;
			if(objData != null && objData.getClass() == Data.getClass()) {
				Context ctx = getContext();
				StyloQApp app_ctx = (ctx != null) ? (StyloQApp)ctx.getApplicationContext() : null;
				if(app_ctx != null) {
					Document.TransferItem _data = null;
					CommonPrereqModule.WareEntry goods_item = null;
					if(Data != null && Data instanceof Document.TransferItem)
						_data = (Document.TransferItem)Data;
					else {
						Data = new Document.TransferItem();
						_data = (Document.TransferItem)Data;
					}
					double _upp = 0.0; // Емкость упаковки
					double _mult = 0.0; // Кратность количества в заказе
					double _min = 0.0;	// Минимальный заказ
					String text = "";
					String blob_signature = null;
					int    uom_id = 0;
					if(_data != null && _data.GoodsID > 0 && ActivityCtx != null) {
						goods_item = ActivityCtx.CPM.FindGoodsItemByGoodsID(_data.GoodsID);
						if(goods_item != null && goods_item.JsItem != null) {
							uom_id = goods_item.JsItem.optInt("uomid", 0);
							text = goods_item.JsItem.optString("nm", "");
							blob_signature =  goods_item.JsItem.optString("imgblobs", null);
							//
							_upp = goods_item.JsItem.optDouble("upp", 0.0);
							_mult = goods_item.JsItem.optDouble("ordqtymult", 0.0);
							_min = goods_item.JsItem.optDouble("ordminqty", 0.0);
						}
					}
					if(_upp > 0.0) {
						String upp_text = app_ctx.GetString("unitperpack_ss") + " " + SLib.formatdouble(_upp, 0);
						SLib.SetCtrlString(this, R.id.CTL_ORDRTI_PACK, upp_text);
					}
					else {
						SLib.SetCtrlVisibility(this, R.id.CTL_ORDRTI_PACK, View.GONE);
					}
					if(_mult > 0.0) {
						String upp_text = app_ctx.GetString("goods_fmultminshipm_ss") + " " + SLib.formatdouble(_mult, 0);
						SLib.SetCtrlString(this, R.id.CTL_ORDRTI_MINORMULT, upp_text);
					}
					else if(_min > 0.0) {
						String upp_text = app_ctx.GetString("goods_minshippmqtty_ss") + " " + SLib.formatdouble(_min, 0);
						SLib.SetCtrlString(this, R.id.CTL_ORDRTI_MINORMULT, upp_text);
					}
					else {
						SLib.SetCtrlVisibility(this, R.id.CTL_ORDRTI_MINORMULT, View.GONE);
					}
					SLib.SetCtrlString(this, R.id.CTL_ORDRTI_GOODSNAME, text);
					SLib.SetCtrlString(this, R.id.CTL_ORDRTI_PRICE, ActivityCtx.CPM.FormatCurrency(_data.Set.Price));
					SLib.SetCtrlString(this, R.id.CTL_ORDRTI_QTTY, ActivityCtx.CPM.FormatQtty(_data.Set.Qtty,  uom_id, true));
					SLib.SetCtrlString(this, R.id.CTL_ORDRTI_AMOUNT, ActivityCtx.CPM.FormatCurrency(_data.Set.Qtty * _data.Set.Price));
					//
					SLib.SetupImage(ActivityCtx, this.findViewById(R.id.CTL_ORDRTI_IMG), blob_signature, false);
				}
			}
			return ok;
		}
		Object GetDTS()
		{
			Object result = null;
			StyloQApp app_ctx = ActivityCtx.GetAppCtx();
			if(app_ctx != null) {
				Document.TransferItem _data = null;
				if(Data != null && Data instanceof Document.TransferItem)
					_data = (Document.TransferItem)Data;
				else {
					_data = new Document.TransferItem();
					Data = _data;
				}
				String qtty_text = SLib.GetCtrlString(this, R.id.CTL_ORDRTI_QTTY);
				if(SLib.GetLen(qtty_text) > 0)
					_data.Set.Qtty = Double.parseDouble(qtty_text);
				result = Data;
			}
			return result;
		}
	}
	int FindClientItemIndexByID(int id)
	{
		int result = -1;
		if(CPM.CliListData != null && id > 0) {
			for(int i = 0; result < 0 && i < CPM.CliListData.size(); i++) {
				final int iter_id = CPM.CliListData.get(i).JsItem.optInt("id", 0);
				if(iter_id == id)
					result = i;
			}
		}
		return result;
	}
	static class TabInitEntry {
		TabInitEntry(final CommonPrereqModule.Tab tab, final int rc, final String title, boolean condition)
		{
			Tab = tab;
			Rc = rc;
			Title = title;
			Condition = condition;
		}
		final CommonPrereqModule.Tab Tab;
		final int Rc;
		final String Title;
		final boolean Condition;
	}
	private void CreateTabList(boolean force)
	{
		final int tab_layout_rcid = R.id.TABLAYOUT_ORDERPREREQ;
		StyloQApp app_ctx = GetAppCtx();
		if(app_ctx != null && (CPM.TabList == null || force)) {
			CPM.TabList = new ArrayList<CommonPrereqModule.TabEntry>();
			LayoutInflater inflater = LayoutInflater.from(this);
			TabInitEntry[] tab_init_list = {
				new TabInitEntry(CommonPrereqModule.Tab.tabGoodsGroups, R.layout.layout_orderprereq_goodsgroups, "@{group_pl}", (CPM.GoodsGroupListData != null)),
				new TabInitEntry(CommonPrereqModule.Tab.tabBrands, R.layout.layout_orderprereq_brands, "@{brand_pl}", (CPM.BrandListData != null)),
				new TabInitEntry(CommonPrereqModule.Tab.tabGoods, R.layout.layout_orderprereq_goods, "@{ware_pl}", (CPM.GoodsListData != null)),
				new TabInitEntry(CommonPrereqModule.Tab.tabClients, R.layout.layout_orderprereq_clients, "@{client_pl}", (CPM.CliListData != null)),
				new TabInitEntry(CommonPrereqModule.Tab.tabCurrentOrder, R.layout.layout_orderprereq_ordr, "@{orderdocument}", true),
				new TabInitEntry(CommonPrereqModule.Tab.tabOrders, R.layout.layout_orderprereq_orders, "@{booking_pl}", true),
				new TabInitEntry(CommonPrereqModule.Tab.tabSearch, R.layout.layout_searchpane, "[search]", true),
			};
			for(int i = 0; i < tab_init_list.length; i++) {
				final TabInitEntry _t = tab_init_list[i];
				if(_t != null && _t.Condition) {
					SLib.SlFragmentStatic f = SLib.SlFragmentStatic.newInstance(_t.Tab.ordinal(), _t.Rc, tab_layout_rcid);
					String title = SLib.ExpandString(app_ctx, _t.Title);
					CPM.TabList.add(new CommonPrereqModule.TabEntry(_t.Tab, title, f));
				}
			}
		}
	}
	private CommonPrereqModule.TabEntry SearchTabEntry(CommonPrereqModule.Tab tab)
	{
		return CPM.SearchTabEntry(R.id.VIEWPAGER_ORDERPREREQ, tab);
	}
	private void GotoTab(CommonPrereqModule.Tab tab, @IdRes int recyclerViewToUpdate, int goToIndex, int nestedIndex)
	{
		CPM.GotoTab(tab, R.id.VIEWPAGER_ORDERPREREQ, recyclerViewToUpdate, goToIndex, nestedIndex);
	}
	private void NotifyTabContentChanged(CommonPrereqModule.Tab tabId, int innerViewId)
	{
		CPM.NotifyTabContentChanged(R.id.VIEWPAGER_ORDERPREREQ, tabId, innerViewId);
	}
	private void NotifyDocListChanged() { NotifyTabContentChanged(CommonPrereqModule.Tab.tabOrders, R.id.orderPrereqOrderListView); }
	private void NotifyCurrentOrderChanged()
	{
		NotifyTabContentChanged(CommonPrereqModule.Tab.tabCurrentOrder, R.id.orderPrereqOrdrListView);
		//NotifyTabContentChanged(CommonPrereqModule.Tab.tabCurrentOrder, R.id.CTL_DOCUMENT_AMOUNT);
		NotifyTabContentChanged(CommonPrereqModule.Tab.tabClients, R.id.orderPrereqClientsListView);
		CommonPrereqModule.TabEntry tab_entry = SearchTabEntry(CommonPrereqModule.Tab.tabCurrentOrder);
		if(tab_entry != null && tab_entry.TabView != null) {
			CPM.OnCurrentDocumentModification(); // @v11.4.8
			HandleEvent(SLib.EV_SETVIEWDATA, tab_entry.TabView.getView(), null);
		}
	}
	private boolean SetCurrentOrderClient(JSONObject cliItem, JSONObject dlvrLocItem)
	{
		boolean result = false;
		//try {
		JSONObject final_cli_js = null;
		if(CPM.CliListData != null) {
			if(dlvrLocItem != null) {
				final_cli_js = CPM.FindClientEntryByDlvrLocID(dlvrLocItem.optInt("id", 0));
			}
			else if(cliItem != null) {
				final_cli_js = cliItem;
				int cli_id = cliItem.optInt("id", 0);
				if(cli_id > 0) {

				}
			}
			if(final_cli_js != null) {
				int cli_id = final_cli_js.optInt("id", 0);
				int dlvrloc_id = (dlvrLocItem != null) ? dlvrLocItem.optInt("id", 0) : 0;
				result = CPM.SetClientToCurrentDocument(SLib.PPEDIOP_ORDER, cli_id, dlvrloc_id, false);
				if(result) {
					CPM.SetTabVisibility(CommonPrereqModule.Tab.tabCurrentOrder, View.VISIBLE);
					NotifyCurrentOrderChanged();
					NotifyTabContentChanged(CommonPrereqModule.Tab.tabClients, R.id.orderPrereqClientsListView);
				}
			}
		}
		//} catch(StyloQException exn) { result = false; }
		return result;
	}
	//
	private boolean AddItemToCurrentOrder(Document.TransferItem item)
	{
		boolean result = CPM.AddTransferItemToCurrentDocument(item);
		if(result)
			NotifyCurrentOrderChanged();
		return result;
	}
	private static class DlvrLocListAdapter extends SLib.InternalArrayAdapter {
		//private int RcId;
		DlvrLocListAdapter(Context ctx, int rcId, ArrayList data)
		{
			super(ctx, rcId, data);
			//RcId = rcId;
		}
		@Override public View getView(int position, View convertView, ViewGroup parent)
		{
			// Get the data item for this position
			Object item = (Object)getItem(position);
			//Context ctx = parent.getContext();
			Context _ctx = getContext();
			if(item != null && _ctx != null) {
				// Check if an existing view is being reused, otherwise inflate the view
				if(convertView == null) {
					convertView = LayoutInflater.from(_ctx).inflate(RcId, parent, false);
				}
				if(convertView != null) {
					TextView v = convertView.findViewById(R.id.LVITEM_GENERICNAME);
					if(v != null) {
						int loc_id = 0;
						if(item instanceof JSONObject) {
							JSONObject js_item = (JSONObject) item;
							loc_id = js_item.optInt("id", 0);
							v.setText(js_item.optString("addr", ""));
						}
						if(_ctx instanceof CmdROrderPrereqActivity)
							((CmdROrderPrereqActivity)_ctx).SetListBackground(convertView, this, position, SLib.PPOBJ_LOCATION, loc_id);
					}
				}
			}
			return convertView; // Return the completed view to render on screen
		}
	}
	private void SetListBackground(View iv, Object adapter, int itemIdxToDraw, int objType, int objID)
	{
		int shaperc = 0;
		if(GetListFocusedIndex(adapter) == itemIdxToDraw)
			shaperc = R.drawable.shape_listitem_focused;
		else {
			boolean is_catched = false;
			if(objID > 0 && !CPM.IsCurrentDocumentEmpty()) {
				final Document _doc = CPM.GetCurrentDocument();
				if(objType == SLib.PPOBJ_PERSON && objID == _doc.H.ClientID) {
					is_catched = true;
				}
				else if(objType == SLib.PPOBJ_LOCATION && objID == _doc.H.DlvrLocID) {
					is_catched = true;
				}
				else if(objType == SLib.PPOBJ_GOODS) {
					if(CPM.HasGoodsInCurrentOrder(objID))
						is_catched = true;
				}
			}
			if(is_catched)
				shaperc = R.drawable.shape_listitem_catched;
			else if(CPM.IsObjInSearchResult(objType, objID))
				shaperc = R.drawable.shape_listitem_found;
			else
				shaperc = R.drawable.shape_listitem;
		}
		iv.setBackground(getResources().getDrawable(shaperc, getTheme()));
	}
	private void GetFragmentData(Object entry)
	{
		if(entry != null) {
			ViewGroup vg = null;
			if(entry instanceof SLib.SlFragmentStatic) {
				View v = ((SLib.SlFragmentStatic)entry).getView();
				if(v instanceof ViewGroup)
					vg = (ViewGroup)v;
			}
			else if(entry instanceof ViewGroup)
				vg = (ViewGroup)entry;
			if(vg != null) {
				int vg_id = vg.getId();
				if(vg_id == R.id.LAYOUT_ORDERPREPREQ_ORDR) {
					boolean umr = CPM.UpdateMemoInCurrentDocument(SLib.GetCtrlString(vg, R.id.CTL_DOCUMENT_MEMO));
					if(umr)
						CPM.OnCurrentDocumentModification();
				}
			}
		}
	}
	private void SetupCurrentDocument(boolean gotoTabIfNotEmpty, boolean removeTabIfEmpty)
	{
		//
		// При попытке скрыть и потом показать табы они перерисовываются с искажениями.
		// по этому не будем злоупотреблять такими фокусами.
		//
		if(!CPM.IsCurrentDocumentEmpty()) {
			CPM.SetTabVisibility(CommonPrereqModule.Tab.tabCurrentOrder, View.VISIBLE);
			if(Document.DoesStatusAllowModifications(CPM.GetCurrentDocument().GetDocStatus())) {
				//CPM.SetTabVisibility(CommonPrereqModule.Tab.tabGoods, View.VISIBLE);
				//CPM.SetTabVisibility(CommonPrereqModule.Tab.tabBrands, View.VISIBLE);
				//CPM.SetTabVisibility(CommonPrereqModule.Tab.tabGoodsGroups, View.VISIBLE);
				//CPM.SetTabVisibility(CommonPrereqModule.Tab.tabClients, View.VISIBLE);
			}
			else {
				//CPM.SetTabVisibility(CommonPrereqModule.Tab.tabGoods, View.GONE);
				//CPM.SetTabVisibility(CommonPrereqModule.Tab.tabBrands, View.GONE);
				//CPM.SetTabVisibility(CommonPrereqModule.Tab.tabGoodsGroups, View.GONE);
				//CPM.SetTabVisibility(CommonPrereqModule.Tab.tabClients, View.GONE);
			}
			if(gotoTabIfNotEmpty)
				GotoTab(CommonPrereqModule.Tab.tabCurrentOrder, R.id.orderPrereqOrdrListView, -1, -1);
		}
		else {
			//CPM.SetTabVisibility(CommonPrereqModule.Tab.tabGoods, View.VISIBLE);
			//CPM.SetTabVisibility(CommonPrereqModule.Tab.tabBrands, View.VISIBLE);
			//CPM.SetTabVisibility(CommonPrereqModule.Tab.tabGoodsGroups, View.VISIBLE);
			//CPM.SetTabVisibility(CommonPrereqModule.Tab.tabClients, View.VISIBLE);
			if(removeTabIfEmpty)
				CPM.SetTabVisibility(CommonPrereqModule.Tab.tabCurrentOrder, View.GONE);
		}
	}
	public Object HandleEvent(int ev, Object srcObj, Object subj)
	{
		Object result = null;
		switch(ev) {
			case SLib.EV_CREATE:
				{
					Intent intent = getIntent();
					try {
						CPM.GetAttributesFromIntent(intent);
						long doc_id = intent.getLongExtra("SvcReplyDocID", 0);
						String svc_reply_doc_json = null;
						StyloQApp app_ctx = GetAppCtx();
						if(app_ctx != null) {
							StyloQDatabase db = app_ctx.GetDB();
							if(doc_id > 0) {
								StyloQDatabase.SecStoragePacket doc_packet = db.GetPeerEntry(doc_id);
								if(doc_packet != null) {
									byte[] raw_doc = doc_packet.Pool.Get(SecretTagPool.tagRawData);
									if(SLib.GetLen(raw_doc) > 0)
										svc_reply_doc_json = new String(raw_doc);
								}
							}
							else
								svc_reply_doc_json = intent.getStringExtra("SvcReplyDocJson");
							if(SLib.GetLen(svc_reply_doc_json) > 0) {
								JSONObject js_head = new JSONObject(svc_reply_doc_json);
								CPM.GetCommonJsonFactors(js_head);
								CPM.MakeUomListFromCommonJson(js_head);
								CPM.MakeGoodsGroupListFromCommonJson(js_head);
								CPM.MakeGoodsListFromCommonJson(js_head);
								CPM.MakeBrandListFromCommonJson(js_head);
								WharehouseListData = js_head.optJSONArray("warehouse_list");
								QuotKindListData = js_head.optJSONArray("quotkind_list");
								CPM.MakeClientListFromCommonJson(js_head);
								CPM.RestoreRecentDraftDocumentAsCurrent(); // @v11.4.0
								CPM.MakeCurrentDocList();
								MakeSimpleSearchIndex();
							}
							requestWindowFeature(Window.FEATURE_NO_TITLE);
							setContentView(R.layout.activity_cmdrorderprereq);
							CPM.SetupActivity(db, R.id.VIEWPAGER_ORDERPREREQ, R.id.TABLAYOUT_ORDERPREREQ);
							ViewPager2 view_pager = (ViewPager2)findViewById(R.id.VIEWPAGER_ORDERPREREQ);
							SetupViewPagerWithFragmentAdapter(R.id.VIEWPAGER_ORDERPREREQ);
							{
								TabLayout lo_tab = findViewById(R.id.TABLAYOUT_ORDERPREREQ);
								if(lo_tab != null) {
									CreateTabList(false);
									for(int i = 0; i < CPM.TabList.size(); i++) {
										TabLayout.Tab tab = lo_tab.newTab();
										tab.setText(CPM.TabList.get(i).TabText);
										lo_tab.addTab(tab);
									}
									SLib.SetupTabLayoutStyle(lo_tab);
									SLib.SetupTabLayoutListener(lo_tab, view_pager);
									if(CPM.IsCurrentDocumentEmpty())
										CPM.SetTabVisibility(CommonPrereqModule.Tab.tabCurrentOrder, View.GONE);
									if(CPM.OrderHList == null || CPM.OrderHList.size() <= 0)
										CPM.SetTabVisibility(CommonPrereqModule.Tab.tabOrders, View.GONE);
									//SetTabVisibility(Tab.tabSearch, View.GONE);
								}
							}
							SLib.SetCtrlVisibility(this, R.id.tbButtonClearFiter, View.GONE);
						}
					} catch(JSONException exn) {
						//exn.printStackTrace();
					} catch(StyloQException exn) {
						//exn.printStackTrace();
					}
				}
				break;
			case SLib.EV_LISTVIEWCOUNT:
				if(srcObj instanceof SLib.FragmentAdapter) {
					CreateTabList(false);
					result = new Integer(CPM.TabList.size());
				}
				else if(srcObj instanceof SLib.RecyclerListAdapter) {
					SLib.RecyclerListAdapter a = (SLib.RecyclerListAdapter)srcObj;
					switch(a.GetListRcId()) {
						case R.id.orderPrereqGoodsListView: result = new Integer(CPM.GetGoodsListSize()); break;
						case R.id.orderPrereqGoodsGroupListView: result = new Integer((CPM.GoodsGroupListData != null) ? CPM.GoodsGroupListData.size() : 0); break;
						case R.id.orderPrereqBrandListView: result = new Integer((CPM.BrandListData != null) ? CPM.BrandListData.size() : 0); break;
						case R.id.orderPrereqOrdrListView: result = new Integer(CPM.GetCurrentDocumentTransferListCount()); break;
						case R.id.orderPrereqOrderListView:
							result = new Integer((CPM.OrderHList != null) ? CPM.OrderHList.size() : 0);
							break;
						case R.id.orderPrereqClientsListView: result = new Integer((CPM.CliListData != null) ? CPM.CliListData.size() : 0); break;
						case R.id.searchPaneListView:
							{
								result = new Integer((CPM.SearchResult != null) ? CPM.SearchResult.GetObjTypeCount() : 0);
								//result = new Integer((SearchResult != null && SearchResult.List != null) ? SearchResult.List.size() : 0);
							}
							break;
					}
				}
				break;
			case SLib.EV_GETVIEWDATA:
				if(srcObj != null && srcObj instanceof ViewGroup)
					GetFragmentData(srcObj);
				break;
			case SLib.EV_SETVIEWDATA:
				if(srcObj != null && srcObj instanceof ViewGroup) {
					StyloQApp app_ctx = GetAppCtx();
					ViewGroup vg = (ViewGroup)srcObj;
					int vg_id = vg.getId();
					if(vg_id == R.id.LAYOUT_ORDERPREPREQ_ORDR) {
						int status_image_rc_id = 0;
						if(CPM.IsCurrentDocumentEmpty()) {
							SLib.SetCtrlString(vg, R.id.CTL_DOCUMENT_CODE, "");
							SLib.SetCtrlString(vg, R.id.CTL_DOCUMENT_DATE, "");
							SLib.SetCtrlString(vg, R.id.CTL_DOCUMENT_CLI, "");
							SLib.SetCtrlString(vg, R.id.CTL_DOCUMENT_DLVRLOC, "");
							SLib.SetCtrlString(vg, R.id.CTL_DOCUMENT_AMOUNT, "");
							SLib.SetCtrlString(vg, R.id.CTL_DOCUMENT_MEMO, "");
							SLib.SetCtrlVisibility(vg, R.id.CTL_DOCUMENT_ACTIONBUTTON1, View.GONE);
							SLib.SetCtrlVisibility(vg, R.id.CTL_DOCUMENT_ACTIONBUTTON2, View.GONE);
							SLib.SetCtrlVisibility(vg, R.id.CTL_DOCUMENT_ACTIONBUTTON3, View.GONE);
							SLib.SetCtrlVisibility(vg, R.id.CTL_DOCUMENT_ACTIONBUTTON4, View.GONE);
							SLib.SetCtrlVisibility(vg, R.id.CTL_DOCUMENT_DUEDATE_NEXT, View.GONE);
							SLib.SetCtrlVisibility(vg, R.id.CTL_DOCUMENT_DUEDATE_PREV, View.GONE);
						}
						else {
							Document _doc = CPM.GetCurrentDocument();
							if(SLib.GetLen(_doc.H.Code) > 0)
								SLib.SetCtrlString(vg, R.id.CTL_DOCUMENT_CODE, _doc.H.Code);
							{
								SLib.LDATE d = _doc.GetNominalDate();
								if(d != null)
									SLib.SetCtrlString(vg, R.id.CTL_DOCUMENT_DATE, d.Format(SLib.DATF_ISO8601 | SLib.DATF_CENTURY));
							}
							// @v11.4.8 {
							{
								if(_doc.H.DueTime == null || !SLib.CheckDate(_doc.H.DueTime.d)) {
									SLib.LDATETIME time_base = _doc.H.GetNominalTimestamp();
									if(time_base != null) {
										if(CPM.GetDefDuePeriodHour() > 0) {
											_doc.H.DueTime = SLib.plusdatetimesec(time_base, CPM.GetDefDuePeriodHour() * 3600);
										}
									}
								}
								if(_doc.H.DueTime != null && SLib.CheckDate(_doc.H.DueTime.d)) {
									SLib.SetCtrlString(vg, R.id.CTL_DOCUMENT_DUEDATE, _doc.H.DueTime.d.Format(SLib.DATF_ISO8601 | SLib.DATF_CENTURY));
								}
								SLib.SetCtrlVisibility(vg, R.id.CTL_DOCUMENT_DUEDATE_NEXT, _doc.H.IncrementDueDate(true) ? View.VISIBLE : View.GONE);
								SLib.SetCtrlVisibility(vg, R.id.CTL_DOCUMENT_DUEDATE_PREV, _doc.H.DecrementDueDate(true) ? View.VISIBLE : View.GONE);
								{
									View btn = vg.findViewById(R.id.CTL_DOCUMENT_DUEDATE_PREV);
									if(btn != null) {
										btn.setOnClickListener(new View.OnClickListener() {
											@Override public void onClick(View v) { HandleEvent(SLib.EV_COMMAND, v, null); }
										});
									}
								}
								{
									View btn = vg.findViewById(R.id.CTL_DOCUMENT_DUEDATE_NEXT);
									if(btn != null) {
										btn.setOnClickListener(new View.OnClickListener() {
											@Override public void onClick(View v) { HandleEvent(SLib.EV_COMMAND, v, null); }
										});
									}
								}
							}
							// } @v11.4.8
							{
								String cli_name = "";
								String addr = "";
								if(_doc.H.ClientID > 0) {
									JSONObject cli_entry = CPM.FindClientEntry(_doc.H.ClientID);
									if(cli_entry != null)
										cli_name = cli_entry.optString("nm", "");
								}
								if(_doc.H.DlvrLocID > 0) {
									JSONObject cli_entry = CPM.FindClientEntryByDlvrLocID(_doc.H.DlvrLocID);
									if(cli_entry != null) {
										JSONObject dlvrlov_entry = CPM.FindDlvrLocEntryInCliEntry(cli_entry, _doc.H.DlvrLocID);
										if(dlvrlov_entry != null)
											addr = dlvrlov_entry.optString("addr", "");
									}
								}
								SLib.SetCtrlString(vg, R.id.CTL_DOCUMENT_CLI, cli_name);
								SLib.SetCtrlString(vg, R.id.CTL_DOCUMENT_DLVRLOC, addr);
							}
							SLib.SetCtrlString(vg, R.id.CTL_DOCUMENT_MEMO, _doc.H.Memo);
							{
								double amount = CPM.GetAmountOfCurrentDocument();
								SLib.SetCtrlString(vg, R.id.CTL_DOCUMENT_AMOUNT, CPM.FormatCurrency(amount));
							}
							{
								DocEditActionList = Document.GetEditActionsConnectedWithStatus(_doc.GetDocStatus());
								if(DocEditActionList != null && DocEditActionList.size() > 0) {
									int acn_idx = 0;
									for(; acn_idx < DocEditActionList.size(); acn_idx++) {
										Document.EditAction acn = DocEditActionList.get(acn_idx);
										switch(acn_idx) {
											case 0:
												SLib.SetCtrlVisibility(vg, R.id.CTL_DOCUMENT_ACTIONBUTTON1, View.VISIBLE);
												SLib.SetCtrlString(vg, R.id.CTL_DOCUMENT_ACTIONBUTTON1, acn.GetTitle(app_ctx));
												break;
											case 1:
												SLib.SetCtrlVisibility(vg, R.id.CTL_DOCUMENT_ACTIONBUTTON2, View.VISIBLE);
												SLib.SetCtrlString(vg, R.id.CTL_DOCUMENT_ACTIONBUTTON2, acn.GetTitle(app_ctx));
												break;
											case 2:
												SLib.SetCtrlVisibility(vg, R.id.CTL_DOCUMENT_ACTIONBUTTON3, View.VISIBLE);
												SLib.SetCtrlString(vg, R.id.CTL_DOCUMENT_ACTIONBUTTON3, acn.GetTitle(app_ctx));
												break;
											case 3:
												SLib.SetCtrlVisibility(vg, R.id.CTL_DOCUMENT_ACTIONBUTTON4, View.VISIBLE);
												SLib.SetCtrlString(vg, R.id.CTL_DOCUMENT_ACTIONBUTTON4, acn.GetTitle(app_ctx));
												break;
										}
									}
									for(; acn_idx < 4; acn_idx++) {
										switch(acn_idx) {
											case 0: SLib.SetCtrlVisibility(vg, R.id.CTL_DOCUMENT_ACTIONBUTTON1, View.GONE); break;
											case 1: SLib.SetCtrlVisibility(vg, R.id.CTL_DOCUMENT_ACTIONBUTTON2, View.GONE); break;
											case 2: SLib.SetCtrlVisibility(vg, R.id.CTL_DOCUMENT_ACTIONBUTTON3, View.GONE); break;
											case 3: SLib.SetCtrlVisibility(vg, R.id.CTL_DOCUMENT_ACTIONBUTTON4, View.GONE); break;
										}
									}
								}
							}
							status_image_rc_id = Document.GetImageResourceByDocStatus(_doc.GetDocStatus());
						}
						{
							//
							View back_cli_img_view = findViewById(R.id.CTL_DOCUMENT_BACK_CLI);
							// Агентские заказы - требуется указание клиента
							if(CPM.GetAgentID() > 0 && CPM.GetCurrentDocument() != null && Document.DoesStatusAllowModifications(CPM.GetCurrentDocument().GetDocStatus())) {
								if(back_cli_img_view != null) {
									back_cli_img_view.setVisibility(ViewGroup.VISIBLE);
									back_cli_img_view.setOnClickListener(new View.OnClickListener() {
										@Override public void onClick(View v) { HandleEvent(SLib.EV_COMMAND, v, null); }
									});
								}
							}
							else
								SLib.SetCtrlVisibility(back_cli_img_view, View.GONE);
						}
						{
							View v = findViewById(R.id.CTL_DOCUMENT_STATUSICON);
							if(v != null && v instanceof ImageView)
								((ImageView)v).setImageResource(status_image_rc_id);
						}
						CPM.DrawCurrentDocumentRemoteOpIndicators();
					}
				}
				break;
			case SLib.EV_GETLISTITEMVIEW:
				{
					SLib.ListViewEvent ev_subj = (subj instanceof SLib.ListViewEvent) ? (SLib.ListViewEvent) subj : null;
					if(ev_subj != null && ev_subj.ItemIdx >= 0) {
						if(ev_subj.RvHolder != null) {
							// RecyclerView
							if(srcObj != null && srcObj instanceof SLib.RecyclerListAdapter) {
								SLib.RecyclerListAdapter a = (SLib.RecyclerListAdapter)srcObj;
								if(a.GetListRcId() == R.id.searchPaneListView) {
									CPM.GetSearchPaneListViewItem(ev_subj.RvHolder.itemView, ev_subj.ItemIdx);
								}
								else if(a.GetListRcId() == R.id.orderPrereqClientsListView) {
									if(CPM.CliListData != null && ev_subj.ItemIdx < CPM.CliListData.size()) {
										View iv = ev_subj.RvHolder.itemView;
										CommonPrereqModule.CliEntry cur_entry = null;
										cur_entry = (CommonPrereqModule.CliEntry)CPM.CliListData.get(ev_subj.ItemIdx);
										final int cur_cli_id = cur_entry.JsItem.optInt("id", 0);
										SLib.SetCtrlString(iv, R.id.LVITEM_GENERICNAME, cur_entry.JsItem.optString("nm", ""));
										SetListBackground(iv, a, ev_subj.ItemIdx, SLib.PPOBJ_PERSON, cur_cli_id);
										{
											ImageView ctl = (ImageView)iv.findViewById(R.id.ORDERPREREQ_CLI_EXPANDSTATUS);
											if(ctl != null) {
												ListView dlvrloc_lv = (ListView)iv.findViewById(R.id.dlvrLocListView);
												ArrayList <JSONObject> dlvr_loc_list = cur_entry.GetDlvrLocListAsArray();
												if(cur_entry.AddrExpandStatus == 0 || dlvr_loc_list == null) {
													ctl.setVisibility(View.INVISIBLE);
													SLib.SetCtrlVisibility(dlvrloc_lv, View.GONE);
												}
												else if(cur_entry.AddrExpandStatus == 1) {
													ctl.setVisibility(View.VISIBLE);
													ctl.setImageResource(R.drawable.ic_triangleleft03);
													SLib.SetCtrlVisibility(dlvrloc_lv, View.GONE);
												}
												else if(cur_entry.AddrExpandStatus == 2) {
													ctl.setVisibility(View.VISIBLE);
													ctl.setImageResource(R.drawable.ic_triangledown03);
													if(dlvrloc_lv != null) {
														dlvrloc_lv.setVisibility(View.VISIBLE);
														DlvrLocListAdapter adapter = new DlvrLocListAdapter(/*this*/iv.getContext(), R.layout.li_simple_sublist, dlvr_loc_list);
														dlvrloc_lv.setAdapter(adapter);
														{
															int total_items_height = SLib.CalcListViewHeight(dlvrloc_lv);
															if(total_items_height > 0) {
																ViewGroup.LayoutParams params = dlvrloc_lv.getLayoutParams();
																params.height = total_items_height;
																dlvrloc_lv.setLayoutParams(params);
																dlvrloc_lv.requestLayout();
															}
														}
														adapter.setNotifyOnChange(true);
														dlvrloc_lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
															@Override public void onItemClick(AdapterView<?> parent, View view, int position, long id)
															{
																Object item = (Object)parent.getItemAtPosition(position);
																Context ctx = parent.getContext();
																if(item != null && ctx != null && ctx instanceof SLib.SlActivity) {
																	SLib.SlActivity activity = (SLib.SlActivity)parent.getContext();
																	SLib.ListViewEvent ev_subj = new SLib.ListViewEvent();
																	ev_subj.ItemIdx = position;
																	ev_subj.ItemId = id;
																	ev_subj.ItemObj = item;
																	ev_subj.ItemView = view;
																	//ev_subj.ParentView = parent;
																	activity.HandleEvent(SLib.EV_LISTVIEWITEMCLK, parent, ev_subj);
																}
															}
														});
													}
												}
											}
										}
									}
								}
								else if(a.GetListRcId() == R.id.orderPrereqGoodsListView) {
									CommonPrereqModule.WareEntry cur_entry = CPM.GetGoodsListItemByIdx(ev_subj.ItemIdx);
									if(cur_entry != null && cur_entry.JsItem != null) {
										//
										// @todo С этим элементом есть проблема: если пользователь на телефоне поставил слишком большое масштабирование
										// экрана, то значок корзинки становится невидимым (уходит за правую грань экрана).
										// 1. Я попробовал сделать горизонтальное скроллирование в layout_orderprereq_goods.xml, но в этом случае
										// вся табличка становится шире и корзинка всегда уходит за границу.
										// 2. В li_orderprereq_goods.xml убирал <View style="@style/FakeView"/>. В этом случае в зависимости от наличия/
										// отсутствия изображения товара раскладка меняется - не красиво. Это можно компенсировать параметром
										// dontRemoveIfNoImg=true в функции SLib.SetupImage() но тогда, если сервис не предоставляет картинки товаров,
										// для полезной информации остается мало места.
										//
										// В общем, пока не ясно как с этим бороться.
										//
										final int cur_id = cur_entry.JsItem.optInt("id", 0);
										View iv = ev_subj.RvHolder.itemView;
										SLib.SetCtrlString(iv, R.id.LVITEM_GENERICNAME, cur_entry.JsItem.optString("nm", ""));
										double val = 0.0;
										val = cur_entry.JsItem.optDouble("price", 0.0);
										SLib.SetCtrlString(iv, R.id.ORDERPREREQ_GOODS_PRICE, (val > 0.0) ? CPM.FormatCurrency(val) : "");
										val = cur_entry.JsItem.optDouble("stock", 0.0);
										SLib.SetCtrlString(iv, R.id.ORDERPREREQ_GOODS_REST, (val > 0.0) ? SLib.formatdouble(val, 0) : "");
										val = CPM.GetGoodsQttyInCurrentDocument(cur_id);
										if(val > 0.0) {
											SLib.SetCtrlVisibility(iv, R.id.ORDERPREREQ_GOODS_ORDEREDQTY, View.VISIBLE);
											SLib.SetCtrlString(iv, R.id.ORDERPREREQ_GOODS_ORDEREDQTY, String.format("%.0f", val));
										}
										else {
											SLib.SetCtrlVisibility(iv, R.id.ORDERPREREQ_GOODS_ORDEREDQTY, View.GONE);
										}
										String blob_signature = cur_entry.JsItem.optString("imgblobs", null);
										SLib.SetupImage(this, iv.findViewById(R.id.ORDERPREREQ_GOODS_IMG), blob_signature, false);
										SetListBackground(iv, a, ev_subj.ItemIdx, SLib.PPOBJ_GOODS, cur_id);
									}
								}
								else if(a.GetListRcId() == R.id.orderPrereqGoodsGroupListView) {
									if(CPM.GoodsGroupListData != null && ev_subj.ItemIdx < CPM.GoodsGroupListData.size()) {
										View iv = ev_subj.RvHolder.itemView;
										JSONObject cur_entry = (JSONObject)CPM.GoodsGroupListData.get(ev_subj.ItemIdx);
										SLib.SetCtrlString(iv, R.id.LVITEM_GENERICNAME, cur_entry.optString("nm", ""));
										SetListBackground(iv, a, ev_subj.ItemIdx, SLib.PPOBJ_GOODSGROUP, cur_entry.optInt("id", 0));
									}
								}
								else if(a.GetListRcId() == R.id.orderPrereqBrandListView) {
									if(CPM.BrandListData != null && ev_subj.ItemIdx < CPM.BrandListData.size()) {
										View iv = ev_subj.RvHolder.itemView;
										BusinessEntity.Brand cur_entry = CPM.BrandListData.get(ev_subj.ItemIdx);
										if(cur_entry != null) {
											SLib.SetCtrlString(iv, R.id.LVITEM_GENERICNAME, cur_entry.Name);
											SetListBackground(iv, a, ev_subj.ItemIdx, SLib.PPOBJ_BRAND, cur_entry.ID);
										}
									}
								}
								else if(a.GetListRcId() == R.id.orderPrereqOrderListView) { // Список зафиксированных заказов
									if(CPM.OrderHList != null && ev_subj.ItemIdx < CPM.OrderHList.size()) {
										View iv = ev_subj.RvHolder.itemView;
										Document.DisplayEntry cur_entry = CPM.OrderHList.get(ev_subj.ItemIdx);
										if(cur_entry != null && cur_entry.H != null) {
											final int _vdlc = VdlDocs.GetCount();
											for(int i = 0; i < _vdlc; i++) {
												View ctl_view = iv.findViewById(i+1);
												if(ctl_view != null) {
													ViewDescriptionList.Item di = VdlDocs.Get(i);
													if(di != null) {
														String text = null;
														if(di.Id == 1) { // indicator image
															if(ctl_view instanceof ImageView) {
																final int ds = StyloQDatabase.SecTable.Rec.GetDocStatus(cur_entry.H.Flags);
																int ir = Document.GetImageResourceByDocStatus(ds);
																if(ir != 0)
																	((ImageView)ctl_view).setImageResource(ir);
															}
														}
														else if(ctl_view instanceof TextView){
															if(di.Id == 2) { // date
																SLib.LDATE d = cur_entry.GetNominalDate();
																if(d != null)
																	text = d.Format(SLib.DATF_DMY);
															}
															else if(di.Id == 3) { // code
																text = cur_entry.H.Code;
															}
															else if(di.Id == 4) { // amount
																text = CPM.FormatCurrency(cur_entry.H.Amount);
															}
															else if(di.Id == 5) { // client
																if(cur_entry.H.ClientID > 0) {
																	JSONObject cli_entry = CPM.FindClientEntry(cur_entry.H.ClientID);
																	if(cli_entry != null)
																		text = cli_entry.optString("nm", "");
																}
															}
															else if(di.Id == 6) { // memo
																text = cur_entry.H.Memo;
															}
															((TextView)ctl_view).setText(text);
														}
													}
												}
											}
											/*{
												SLib.SetCtrlString(iv, R.id.CTL_DOCUMENT_CODE, (SLib.GetLen(cur_entry.Code) > 0) ? cur_entry.Code : "");
												{
													SLib.LDATE d = null;
													if(cur_entry.Time > 0)
														d = SLib.BuildDateByEpoch(cur_entry.Time);
													else if(cur_entry.CreationTime > 0)
														d = SLib.BuildDateByEpoch(cur_entry.CreationTime);
													if(d != null)
														SLib.SetCtrlString(iv, R.id.CTL_DOCUMENT_DATE, d.Format(SLib.DATF_ISO8601 | SLib.DATF_CENTURY));
												}
												{
													String amount_text = String.format(Locale.US, "%12.2f", cur_entry.Amount);
													SLib.SetCtrlString(iv, R.id.CTL_DOCUMENT_AMOUNT, amount_text);
												}
											}*/
										}
									}
								}
								else if(a.GetListRcId() == R.id.orderPrereqOrdrListView) { // Текущий заказ (точнее, его строки)
									final int cc = CPM.GetCurrentDocumentTransferListCount();
									if(ev_subj.ItemIdx < cc) {
										View iv = ev_subj.RvHolder.itemView;
										final Document _doc = CPM.GetCurrentDocument();
										final Document.TransferItem ti = _doc.TiList.get(ev_subj.ItemIdx);
										if(ti != null) {
											CommonPrereqModule.WareEntry goods_item = CPM.FindGoodsItemByGoodsID(ti.GoodsID);
											int    uom_id = 0;
											if(goods_item != null && goods_item.JsItem != null)
												uom_id = goods_item.JsItem.optInt("uomid", 0);
											SLib.SetCtrlString(iv, R.id.LVITEM_GENERICNAME, (goods_item != null) ? goods_item.JsItem.optString("nm", "") : "");
											SLib.SetCtrlString(iv, R.id.ORDERPREREQ_TI_PRICE, (ti.Set != null) ? CPM.FormatCurrency(ti.Set.Price) : "");
											SLib.SetCtrlString(iv, R.id.ORDERPREREQ_TI_QTTY, (ti.Set != null) ? CPM.FormatQtty(ti.Set.Qtty, uom_id, false) : "");
											double item_amont = (ti.Set != null) ? (ti.Set.Qtty * ti.Set.Price) : 0.0;
											SLib.SetCtrlString(iv, R.id.ORDERPREREQ_TI_AMOUNT, " = " + CPM.FormatCurrency(item_amont));
										}
									}
								}
								else if(a.GetListRcId() == R.id.orderPrereqOrderListView) {
								}
							}
						}
						else {
							;
						}
					}
				}
				break;
			case SLib.EV_CREATEFRAGMENT:
				if(subj instanceof Integer) {
					int item_idx = (Integer)subj;
					if(CPM.TabList != null && item_idx >= 0 && item_idx < CPM.TabList.size()) {
						CommonPrereqModule.TabEntry cur_entry = (CommonPrereqModule.TabEntry)CPM.TabList.get(item_idx);
						if(cur_entry.TabView != null)
							result = cur_entry.TabView;
					}
				}
				break;
			case SLib.EV_SETUPFRAGMENT:
				if(subj != null && subj instanceof View) {
					final int selected_search_idx = (CPM.SearchResult != null) ? CPM.SearchResult.GetSelectedItemIndex() : -1;
					final int selected_search_objtype = (selected_search_idx >= 0) ? CPM.SearchResult.List.get(selected_search_idx).ObjType : 0;
					final int selected_search_objid = (selected_search_idx >= 0) ? CPM.SearchResult.List.get(selected_search_idx).ObjID : 0;
					if(srcObj != null && srcObj instanceof SLib.SlFragmentStatic) {
						SLib.SlFragmentStatic fragment = (SLib.SlFragmentStatic)srcObj;
						View fv = (View)subj;
						View lv = fv.findViewById(R.id.orderPrereqGoodsListView);
						if(lv != null) {
							((RecyclerView) lv).setLayoutManager(new LinearLayoutManager(this));
							SetupRecyclerListView(fv, R.id.orderPrereqGoodsListView, R.layout.li_orderprereq_goods);
							if(selected_search_objtype == SLib.PPOBJ_GOODS) {
								final int foc_idx = CPM.FindGoodsItemIndexByID(selected_search_objid);
								SetRecyclerListFocusedIndex(((RecyclerView) lv).getAdapter(), foc_idx);
								SLib.RequestRecyclerListViewPosition((RecyclerView) lv, foc_idx);
								CPM.SearchResult.ResetSelectedItemIndex();
							}
						}
						else {
							lv = fv.findViewById(R.id.orderPrereqOrderListView);
							if(lv != null) {
								StyloQApp app_ctx = GetAppCtx();
								if(app_ctx != null) {
									SLib.Margin fld_mrgn = new SLib.Margin(8, 12, 8, 12);
									VdlDocs = new ViewDescriptionList();
									{ // #0
										ViewDescriptionList.Item col = new ViewDescriptionList.Item();
										col.Id = 1;
										col.Flags |= ViewDescriptionList.Item.fImage;
										col.FixedWidth = 32;
										col.FixedHeight = 32;
										col.Mrgn = fld_mrgn;
										VdlDocs.AddItem(col);
									}
									{ // #1
										ViewDescriptionList.Item col = new ViewDescriptionList.Item();
										col.Id = 2;
										col.Title = app_ctx.GetString("billdate");
										col.StyleRcId = R.style.OrderListItemText;
										col.Mrgn = fld_mrgn;
										VdlDocs.AddItem(col);
									}
									{ // #2
										ViewDescriptionList.Item col = new ViewDescriptionList.Item();
										col.Id = 3;
										col.Title = app_ctx.GetString("billno");
										col.TotalFunc = SLib.AGGRFUNC_COUNT;
										col.StyleRcId = R.style.OrderListItemText;
										col.Mrgn = fld_mrgn;
										VdlDocs.AddItem(col);
									}
									{ // #3
										ViewDescriptionList.Item col = new ViewDescriptionList.Item();
										col.Id = 4;
										col.Title = app_ctx.GetString("billamount");
										col.TotalFunc = SLib.AGGRFUNC_SUM;
										col.StyleRcId = R.style.OrderListItemText;
										col.Mrgn = fld_mrgn;
										col.ForceAlignment = -1;
										VdlDocs.AddItem(col);
									}
									if(CPM.GetAgentID() > 0) { // Агентские заказы - требуется указание клиента
										{ // #4
											ViewDescriptionList.Item col = new ViewDescriptionList.Item();
											col.Id = 5;
											col.Title = app_ctx.GetString("client");
											col.StyleRcId = R.style.OrderListItemText;
											col.Mrgn = fld_mrgn;
											VdlDocs.AddItem(col);
										}
									}
									{ // #5|#4
										ViewDescriptionList.Item col = new ViewDescriptionList.Item();
										col.Id = 6;
										col.Title = app_ctx.GetString("memo");
										col.StyleRcId = R.style.OrderListItemText;
										col.Mrgn = fld_mrgn;
										VdlDocs.AddItem(col);
									}
									if(CPM.OrderHList != null && CPM.OrderHList.size() > 0) {
										final int _vdlc = VdlDocs.GetCount();
										assert (_vdlc > 0);
										for(int i = 0; i < _vdlc; i++) {
											ViewDescriptionList.DataPreprocessBlock dpb = VdlDocs.StartDataPreprocessing(this, i);
											if(dpb != null && dpb.ColumnDescription != null) {
												for(int j = 0; j < CPM.OrderHList.size(); j++) {
													Document.DisplayEntry cur_entry = CPM.OrderHList.get(j);
													if(cur_entry != null && cur_entry.H != null) {
														String text = null;
														if(dpb.ColumnDescription.Id == 1) { // status image
															; // По-моему, здесь ничего замерять не надо - мы и так зафиксировали размер элемента
														}
														else if(dpb.ColumnDescription.Id == 2) { // date
															SLib.LDATE d = cur_entry.GetNominalDate();
															if(d != null)
																VdlDocs.DataPreprocessingIter(dpb, d.Format(SLib.DATF_DMY));
														}
														else if(dpb.ColumnDescription.Id == 3) { // code
															VdlDocs.DataPreprocessingIter(dpb, cur_entry.H.Code);
														}
														else if(dpb.ColumnDescription.Id == 4) { // amount
															text = CPM.FormatCurrency(cur_entry.H.Amount);
															VdlDocs.DataPreprocessingIter(dpb, new Double(cur_entry.H.Amount), text);
														}
														else if(dpb.ColumnDescription.Id == 5) { // client
															if(cur_entry.H.ClientID > 0) {
																JSONObject cli_entry = CPM.FindClientEntry(cur_entry.H.ClientID);
																if(cli_entry != null)
																	text = cli_entry.optString("nm", "");
															}
															VdlDocs.DataPreprocessingIter(dpb, text);
														}
														else if(dpb.ColumnDescription.Id == 6) { // memo
															VdlDocs.DataPreprocessingIter(dpb, null, cur_entry.H.Memo);
														}
													}
												}
												VdlDocs.FinishDataProcessing(dpb);
												dpb = null;
											}
										}
										{
											LinearLayout header_layout = (LinearLayout) fv.findViewById(R.id.orderPrereqOrderListHeader);
											if(header_layout != null) {
												LinearLayout _lo_ = ViewDescriptionList.CreateItemLayout(VdlDocs, this, 1);
												if(_lo_ != null)
													header_layout.addView(_lo_);
											}
											if(VdlDocs.IsThereTotals()) {
												LinearLayout bottom_layout = (LinearLayout) fv.findViewById(R.id.orderPrereqOrderListBottom);
												if(bottom_layout != null) {
													LinearLayout _lo_ = ViewDescriptionList.CreateItemLayout(VdlDocs, this, 2);
													if(_lo_ != null)
														bottom_layout.addView(_lo_);
												}
											}
										}
									}
									((RecyclerView) lv).setLayoutManager(new LinearLayoutManager(this));
									SetupRecyclerListView(fv, R.id.orderPrereqOrderListView, /*R.layout.li_orderprereq_order*/0);
								}
							}
							else {
								lv = fv.findViewById(R.id.orderPrereqGoodsGroupListView);
								if(lv != null) {
									((RecyclerView) lv).setLayoutManager(new LinearLayoutManager(this));
									SetupRecyclerListView(fv, R.id.orderPrereqGoodsGroupListView, R.layout.li_simple);
									if(selected_search_objtype == SLib.PPOBJ_GOODSGROUP) {
										final int foc_idx = CPM.FindGoodsGroupItemIndexByID(selected_search_objid);
										SetRecyclerListFocusedIndex(((RecyclerView) lv).getAdapter(), foc_idx);
										SLib.RequestRecyclerListViewPosition((RecyclerView) lv, foc_idx);
										CPM.SearchResult.ResetSelectedItemIndex();
									}
								}
								else {
									lv = fv.findViewById(R.id.orderPrereqBrandListView);
									if(lv != null) {
										((RecyclerView) lv).setLayoutManager(new LinearLayoutManager(this));
										SetupRecyclerListView(fv, R.id.orderPrereqBrandListView, R.layout.li_simple);
										if(selected_search_objtype == SLib.PPOBJ_BRAND) {
											final int foc_idx = CPM.FindBrandItemIndexByID(selected_search_objid);
											SetRecyclerListFocusedIndex(((RecyclerView) lv).getAdapter(), foc_idx);
											SLib.RequestRecyclerListViewPosition((RecyclerView) lv, foc_idx);
											CPM.SearchResult.ResetSelectedItemIndex();
										}
									}
									else {
										lv = fv.findViewById(R.id.orderPrereqOrdrListView);
										if(lv != null) {
											((RecyclerView) lv).setLayoutManager(new LinearLayoutManager(this));
											SetupRecyclerListView(fv, R.id.orderPrereqOrdrListView, R.layout.li_orderprereq_ordrti);
										}
										else {
											lv = fv.findViewById(R.id.orderPrereqOrderListView);
											if(lv != null) {
												((RecyclerView) lv).setLayoutManager(new LinearLayoutManager(this));
												SetupRecyclerListView(fv, R.id.orderPrereqOrderListView, R.layout.li_orderprereq_order);
											}
											else {
												lv = fv.findViewById(R.id.orderPrereqClientsListView);
												if(lv != null) {
													((RecyclerView) lv).setLayoutManager(new LinearLayoutManager(this));
													SetupRecyclerListView(fv, R.id.orderPrereqClientsListView, R.layout.li_orderprereq_client);
													if(selected_search_objtype == SLib.PPOBJ_PERSON) {
														SLib.RequestRecyclerListViewPosition((RecyclerView) lv, FindClientItemIndexByID(selected_search_objid));
														CPM.SearchResult.ResetSelectedItemIndex();
													}
													else if(selected_search_objtype == SLib.PPOBJ_LOCATION) {
														// @todo
													}
												}
												else {
													lv = fv.findViewById(R.id.searchPaneListView);
													if(lv != null) {
														((RecyclerView) lv).setLayoutManager(new LinearLayoutManager(this));
														SetupRecyclerListView(fv, R.id.searchPaneListView, R.layout.li_searchpane_result);
														{
															View iv = fv.findViewById(R.id.CTL_SEARCHPANE_INPUT);
															if(iv != null && iv instanceof TextInputEditText) {
																TextInputEditText tiv = (TextInputEditText) iv;
																tiv.requestFocus();
																tiv.addTextChangedListener(new TextWatcher() {
																	public void afterTextChanged(Editable s)
																	{
																		//int cross_icon_id = (s.length() > 0) ? R.drawable.ic_cross01 : 0;
																		//tiv.setCompoundDrawablesWithIntrinsicBounds(0, 0, cross_icon_id, 0);
																	}
																	public void beforeTextChanged(CharSequence s, int start, int count, int after)
																	{
																	}
																	public void onTextChanged(CharSequence s, int start, int before, int count)
																	{
																		String pattern = s.toString();
																		boolean sr = CPM.SearchInSimpleIndex(pattern);
																		String srit = CPM.SearchResult.GetSearchResultInfoText();
																		if(!sr && CPM.SearchResult != null)
																			CPM.SearchResult.Clear();
																		SLib.SetCtrlString(fv, R.id.CTL_SEARCHPANE_RESULTINFO, srit);
																		View lv = findViewById(R.id.searchPaneListView);
																		if(lv != null && lv instanceof RecyclerView) {
																			RecyclerView.Adapter gva = ((RecyclerView) lv).getAdapter();
																			if(gva != null)
																				gva.notifyDataSetChanged();
																		}
																	}
																});
															}
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
				break;
			case SLib.EV_CREATEVIEWHOLDER:
				{
					SLib.ListViewEvent ev_subj = (subj != null && subj instanceof SLib.ListViewEvent) ? (SLib.ListViewEvent) subj : null;
					if(ev_subj != null) {
						if(ev_subj.RvHolder == null) {
							if(ev_subj.ItemView != null && ev_subj.ItemView.getId() == R.id.orderPrereqOrderListView) {
								LinearLayout _lo = ViewDescriptionList.CreateItemLayout(VdlDocs, this,0);
								if(_lo != null) {
									SLib.RecyclerListAdapter adapter = (srcObj != null && srcObj instanceof SLib.RecyclerListAdapter) ? (SLib.RecyclerListAdapter)srcObj : null;
									result = new SLib.RecyclerListViewHolder(_lo, adapter);
								}
							}
						}
						else {
							SLib.SetupRecyclerListViewHolderAsClickListener(ev_subj.RvHolder, ev_subj.ItemView, R.id.buttonOrder);
							SLib.SetupRecyclerListViewHolderAsClickListener(ev_subj.RvHolder, ev_subj.ItemView, R.id.ORDERPREREQ_CLI_EXPANDSTATUS);
							result = ev_subj.RvHolder;
						}
					}
					//
				}
				break;
			case SLib.EV_LISTVIEWITEMCLK:
				{
					SLib.ListViewEvent ev_subj = (subj instanceof SLib.ListViewEvent) ? (SLib.ListViewEvent)subj : null;
					if(ev_subj != null && srcObj != null) {
						if(ev_subj.RvHolder == null) {
							if(srcObj instanceof ListView) {
								if(((ListView)srcObj).getId() == R.id.searchPaneTerminalListView) {
									if(ev_subj.ItemObj != null && ev_subj.ItemObj instanceof CommonPrereqModule.SimpleSearchIndexEntry) {
										CommonPrereqModule.SimpleSearchIndexEntry se = (CommonPrereqModule.SimpleSearchIndexEntry)ev_subj.ItemObj;
										// ! ev_subj.ItemIdx не согласуется простым образом с ev_subj.ItemObj из-за
										// двухярусной структуры списка.
										CPM.SearchResult.SetSelectedItemIndex(CPM.SearchResult.FindIndexOfItem(se));
										if(se.ObjType == SLib.PPOBJ_GOODS) {
											int _idx = CPM.FindGoodsItemIndexByID(se.ObjID);
											GotoTab(CommonPrereqModule.Tab.tabGoods, R.id.orderPrereqGoodsListView, _idx, -1);
										}
										else if(se.ObjType == SLib.PPOBJ_PERSON) {
											int _idx = FindClientItemIndexByID(se.ObjID);
											GotoTab(CommonPrereqModule.Tab.tabClients, R.id.orderPrereqClientsListView, _idx, -1);
										}
										else if(se.ObjType == SLib.PPOBJ_LOCATION) {
											JSONObject cli_js_obj = CPM.FindClientEntryByDlvrLocID(se.ObjID);
											if(cli_js_obj != null) {
												int cli_id = cli_js_obj.optInt("id", 0);
												if(cli_id > 0) {
													int _idx = FindClientItemIndexByID(cli_id);
													int _dlvr_loc_idx = CPM.FindDlvrLocEntryIndexInCliEntry(cli_js_obj, se.ObjID);
													GotoTab(CommonPrereqModule.Tab.tabClients, R.id.orderPrereqClientsListView, _idx, _dlvr_loc_idx);
												}
											}
											//tab_to_select = Tab.tabClients;
										}
										else if(se.ObjType == SLib.PPOBJ_GOODSGROUP) {
											int _idx = CPM.FindGoodsGroupItemIndexByID(se.ObjID);
											GotoTab(CommonPrereqModule.Tab.tabGoodsGroups, R.id.orderPrereqGoodsGroupListView, _idx, -1);
										}
										else if(se.ObjType == SLib.PPOBJ_BRAND) {
											int _idx = CPM.FindBrandItemIndexByID(se.ObjID);
											GotoTab(CommonPrereqModule.Tab.tabBrands, R.id.orderPrereqBrandListView, _idx, -1);
										}
									}
								}
								else if(((ListView)srcObj).getId() == R.id.dlvrLocListView) {
									if(ev_subj.ItemObj != null && ev_subj.ItemObj instanceof JSONObject) {
										if(SetCurrentOrderClient(null, (JSONObject)ev_subj.ItemObj)) {
											GotoTab(CommonPrereqModule.Tab.tabCurrentOrder, R.id.orderPrereqOrdrListView, -1, -1);
										}
									}
								}
							}
						}
						else if(srcObj instanceof SLib.RecyclerListAdapter) {
							SLib.RecyclerListAdapter a = (SLib.RecyclerListAdapter)srcObj;
							StyloQApp app_ctx = GetAppCtx();
							boolean do_update_goods_list_and_toggle_to_it = false;
							final int rc_id = a.GetListRcId();
							if(app_ctx != null && ev_subj.ItemIdx >= 0) {
								switch(rc_id) {
									case R.id.orderPrereqGoodsListView:
										if(ev_subj.ItemIdx < CPM.GetGoodsListSize()) {
											CommonPrereqModule.WareEntry item = CPM.GetGoodsListItemByIdx(ev_subj.ItemIdx);
											if(item != null && ev_subj.ItemView != null && ev_subj.ItemView.getId() == R.id.buttonOrder) {
												final int goods_id = item.JsItem.optInt("id", 0);
												Document.TransferItem ex_ti = CPM.SearchGoodsItemInCurrentOrderTi(goods_id);
												if(ex_ti != null) {
													TransferItemDialog dialog = new TransferItemDialog(this, ex_ti);
													dialog.show();
												}
												else {
													Document.TransferItem ti = new Document.TransferItem();
													if(ti != null) {
														ti.GoodsID = goods_id;
														ti.Set.Price = item.JsItem.optDouble("price", 0.0);
														TransferItemDialog dialog = new TransferItemDialog(this, ti);
														dialog.show();
													}
												}
											}
										}
										break;
									case R.id.orderPrereqClientsListView:
										if(CPM.CliListData != null && ev_subj.ItemIdx < CPM.CliListData.size()) {
											CommonPrereqModule.CliEntry item = CPM.CliListData.get(ev_subj.ItemIdx);
											if(item != null && ev_subj.ItemView != null) {
												if(ev_subj.ItemView.getId() == R.id.ORDERPREREQ_CLI_EXPANDSTATUS) {
													// change expand status
													if(item.AddrExpandStatus == 1) {
														item.AddrExpandStatus = 2;
														a.notifyItemChanged(ev_subj.ItemIdx);
													}
													else if(item.AddrExpandStatus == 2) {
														item.AddrExpandStatus = 1;
														a.notifyItemChanged(ev_subj.ItemIdx);
													}
												}
												else {
													// @v11.4.8 {
													ArrayList <JSONObject> dlvr_loc_list = item.GetDlvrLocListAsArray();
													if(dlvr_loc_list == null || dlvr_loc_list.size() == 0) {
														// У контрагента нет адресов доставки - можно выбрать просто заголовочную запись
														if(SetCurrentOrderClient(item.JsItem, null)) {
															GotoTab(CommonPrereqModule.Tab.tabCurrentOrder, R.id.orderPrereqOrdrListView, -1, -1);
														}
													}
													// } @v11.4.8
												}
											}
										}
										break;
									case R.id.orderPrereqBrandListView:
										if(CPM.BrandListData != null && ev_subj.ItemIdx < CPM.BrandListData.size()) {
											final int brand_id = CPM.BrandListData.get(ev_subj.ItemIdx).ID;
											if(CPM.SetGoodsFilterByBrand(brand_id)) {
												SLib.SetCtrlVisibility(this, R.id.tbButtonClearFiter, View.VISIBLE);
												do_update_goods_list_and_toggle_to_it = true;
											}
										}
										break;
									case R.id.orderPrereqGoodsGroupListView:
										if(CPM.GoodsGroupListData != null && ev_subj.ItemIdx < CPM.GoodsGroupListData.size()) {
											final int group_id = CPM.GoodsGroupListData.get(ev_subj.ItemIdx).optInt("id", 0);
											if(CPM.SetGoodsFilterByGroup(group_id)) {
												SLib.SetCtrlVisibility(this, R.id.tbButtonClearFiter, View.VISIBLE);
												do_update_goods_list_and_toggle_to_it = true;
											}
										}
										break;
									case R.id.orderPrereqOrdrListView:
										final int cc = CPM.GetCurrentDocumentTransferListCount();
										if(ev_subj.ItemIdx < cc) {
											final Document _doc = CPM.GetCurrentDocument();
											final Document.TransferItem ti = _doc.TiList.get(ev_subj.ItemIdx);
											if(ti != null) {
												TransferItemDialog dialog = new TransferItemDialog(this, ti);
												dialog.show();
											}
										}
										break;
									case R.id.orderPrereqOrderListView:
										if(CPM.OrderHList != null && ev_subj.ItemIdx < CPM.OrderHList.size()) {
											Document.DisplayEntry entry = CPM.OrderHList.get(ev_subj.ItemIdx);
											if(entry != null) {
												if(CPM.LoadDocument(entry.H.ID)) {
													SetupCurrentDocument(true, false);
												}
											}
										}
										break;
								}
							}
							if(do_update_goods_list_and_toggle_to_it) {
								GotoTab(CommonPrereqModule.Tab.tabGoods, R.id.orderPrereqGoodsListView, -1, -1);
							}
						}
					}
				}
				break;
			case SLib.EV_COMMAND:
				{
					Document.EditAction acn = null;
					final int view_id = View.class.isInstance(srcObj) ? ((View) srcObj).getId() : 0;
					if(view_id == R.id.tbButtonSearch) {
						GotoTab(CommonPrereqModule.Tab.tabSearch, 0, -1, -1);
					}
					else if(view_id == R.id.tbButtonClearFiter) {
						CPM.ResetGoodsFiter();
						SLib.SetCtrlVisibility(this, R.id.tbButtonClearFiter, View.GONE);
						GotoTab(CommonPrereqModule.Tab.tabGoods, R.id.orderPrereqGoodsListView, -1, -1);
					}
					else if(view_id == R.id.CTL_DOCUMENT_DUEDATE_NEXT) {
						CommonPrereqModule.TabEntry te = SearchTabEntry(CommonPrereqModule.Tab.tabCurrentOrder);
						if(te != null) {
							Document cd = CPM.GetCurrentDocument();
							if(cd != null && cd.H != null && cd.H.IncrementDueDate(false)) {
								GetFragmentData(te.TabView); // @v11.4.8
								NotifyCurrentOrderChanged();
							}
						}
					}
					else if(view_id == R.id.CTL_DOCUMENT_DUEDATE_PREV) {
						CommonPrereqModule.TabEntry te = SearchTabEntry(CommonPrereqModule.Tab.tabCurrentOrder);
						if(te != null) {
							Document cd = CPM.GetCurrentDocument();
							if(cd != null && cd.H != null && cd.H.DecrementDueDate(false)) {
								GetFragmentData(te.TabView); // @v11.4.8
								NotifyCurrentOrderChanged();
							}
						}
					}
					else if(view_id == R.id.CTL_DOCUMENT_ACTIONBUTTON1) {
						if(DocEditActionList != null && DocEditActionList.size() > 0)
							acn = DocEditActionList.get(0);
					}
					else if(view_id == R.id.CTL_DOCUMENT_ACTIONBUTTON2) {
						if(DocEditActionList != null && DocEditActionList.size() > 1)
							acn = DocEditActionList.get(1);
					}
					else if(view_id == R.id.CTL_DOCUMENT_ACTIONBUTTON3) {
						if(DocEditActionList != null && DocEditActionList.size() > 2)
							acn = DocEditActionList.get(2);
					}
					else if(view_id == R.id.CTL_DOCUMENT_ACTIONBUTTON4) {
						if(DocEditActionList != null && DocEditActionList.size() > 3)
							acn = DocEditActionList.get(3);
					}
					else if(view_id == R.id.CTL_DOCUMENT_BACK_CLI) {
						final Document cd = CPM.GetCurrentDocument();
						if(cd != null && cd.H != null) {
							if(Document.DoesStatusAllowModifications(cd.GetDocStatus())) {
								//cd.H.ClientID > 0 &&
								JSONObject cli_js_obj = CPM.FindClientEntry(cd.H.ClientID);
								if(cli_js_obj != null) {
									int _idx = FindClientItemIndexByID(cd.H.ClientID);
									int _dlvr_loc_idx = (cd.H.DlvrLocID > 0) ? CPM.FindDlvrLocEntryIndexInCliEntry(cli_js_obj, cd.H.DlvrLocID) : 0;
									GotoTab(CommonPrereqModule.Tab.tabClients, R.id.orderPrereqClientsListView, _idx, _dlvr_loc_idx);
								}
								else
									GotoTab(CommonPrereqModule.Tab.tabClients, R.id.orderPrereqClientsListView, -1, -1);
							}
						}
					}
					if(acn != null) {
						switch(acn.Action) {
							case Document.editactionClose:
								// Просто закрыть сеанс редактирования документа (изменения и передача сервису не предполагаются)
								CPM.ResetCurrentDocument();
								NotifyCurrentOrderChanged();
								GotoTab(CommonPrereqModule.Tab.tabOrders, R.id.orderPrereqOrderListView, -1, -1);
								//CPM.SetTabVisibility(CommonPrereqModule.Tab.tabCurrentOrder, View.GONE);
								SetupCurrentDocument(false, true);
								break;
							case Document.editactionSubmit:
								// store document; // Подтвердить изменения документа (передача сервису не предполагается)
								break;
							case Document.editactionSubmitAndTransmit:
								{
									// Подтвердить изменения документа с передачей сервису
									CommonPrereqModule.TabEntry te = SearchTabEntry(CommonPrereqModule.Tab.tabCurrentOrder);
									if(te != null)
										GetFragmentData(te.TabView);
									ScheduleRTmr(new RefreshTimerTask(), 1000, 750);
									CPM.CommitCurrentDocument();
								}
								break;
							case Document.editactionCancelEdition:
								// Отменить изменения документа (передача сервису не предполагается)
								CPM.ResetCurrentDocument();
								NotifyCurrentOrderChanged();
								GotoTab(CommonPrereqModule.Tab.tabOrders, R.id.orderPrereqOrderListView, -1, -1);
								//CPM.SetTabVisibility(CommonPrereqModule.Tab.tabCurrentOrder, View.GONE);
								SetupCurrentDocument(false, true);
								break;
							case Document.editactionCancelDocument:
								{
									// Отменить документ с передачей сервису факта отмены
									CommonPrereqModule.TabEntry te = SearchTabEntry(CommonPrereqModule.Tab.tabCurrentOrder);
									if(te != null)
										GetFragmentData(te.TabView);
									ScheduleRTmr(new RefreshTimerTask(), 1000, 750);
									CPM.CancelCurrentDocument();
								}
								break;
						}
					}
				}
				break;
			case SLib.EV_IADATAEDITCOMMIT:
				if(srcObj != null) {
					if(srcObj instanceof TransferItemDialog) {
						if(subj != null && subj instanceof Document.TransferItem) {
							Document.TransferItem _data = (Document.TransferItem) subj;
							boolean do_notify_goods_list = false;
							if(_data.RowIdx == 0) {
								if(AddItemToCurrentOrder(_data))
									do_notify_goods_list = true;
							}
							else {
								if(CPM.UpdateTransferItemQttyInCurrentDocument(_data)) {
									NotifyCurrentOrderChanged();
									do_notify_goods_list = true;
								}
							}
							if(do_notify_goods_list) {
								CommonPrereqModule.TabEntry te = SearchTabEntry(CommonPrereqModule.Tab.tabGoods);
								if(te != null && te.TabView != null) {
									View v = te.TabView.getView();
									if(v != null && v instanceof ViewGroup) {
										View lv = ((ViewGroup) v).findViewById(R.id.orderPrereqGoodsListView);
										if(lv != null && lv instanceof RecyclerView) {
											RecyclerView.Adapter gva = ((RecyclerView) lv).getAdapter();
											if(gva != null)
												gva.notifyDataSetChanged(); // @todo Здесь надо обновлять только одну строку списка товаров
										}
									}
								}
							}
						}
					}
					else if(srcObj instanceof SLib.ConfirmDialog) {

					}
				}
				break;
			case SLib.EV_SVCQUERYRESULT:
				if(subj != null && subj instanceof StyloQApp.InterchangeResult) {
					StyloQApp.InterchangeResult ir = (StyloQApp.InterchangeResult)subj;
					StyloQApp app_ctx = GetAppCtx();
					if(ir.OriginalCmdItem != null) {
						if(ir.OriginalCmdItem.Name.equalsIgnoreCase("PostDocument")) {
							CPM.CurrentDocument_RemoteOp_Finish();
							ScheduleRTmr(null, 0, 0);
							if(ir.ResultTag == StyloQApp.SvcQueryResult.SUCCESS) {
								CPM.MakeCurrentDocList();
								CPM.ResetCurrentDocument();
								NotifyCurrentOrderChanged();
								if(CPM.OrderHList != null && CPM.OrderHList.size() > 0) {
									CPM.SetTabVisibility(CommonPrereqModule.Tab.tabOrders, View.VISIBLE);
									NotifyTabContentChanged(CommonPrereqModule.Tab.tabOrders, R.id.orderPrereqOrderListView);
									GotoTab(CommonPrereqModule.Tab.tabOrders, R.id.orderPrereqOrderListView, -1, -1);
								}
								CPM.SetTabVisibility(CommonPrereqModule.Tab.tabCurrentOrder, View.GONE);
							}
							else {
								String err_msg = app_ctx.GetString(ppstr2.PPSTR_ERROR, ppstr2.PPERR_STQ_POSTDOCUMENTFAULT);
								String reply_err_msg = null;
								if(ir.InfoReply != null && ir.InfoReply instanceof SecretTagPool) {
									JSONObject js_reply = ((SecretTagPool)ir.InfoReply).GetJsonObject(SecretTagPool.tagRawData);
									if(js_reply != null) {
										StyloQInterchange.CommonReplyResult crr = StyloQInterchange.GetReplyResult(js_reply);
										reply_err_msg = crr.ErrMsg;
									}
								}
								if(SLib.GetLen(reply_err_msg) > 0)
									err_msg += ": " + reply_err_msg;
								app_ctx.DisplayError(this, err_msg, 0);
							}
						}
						else if(ir.OriginalCmdItem.Name.equalsIgnoreCase("CancelDocument")) {
							CPM.CurrentDocument_RemoteOp_Finish();
							ScheduleRTmr(null, 0, 0);
							if(ir.ResultTag == StyloQApp.SvcQueryResult.SUCCESS) {
								CPM.MakeCurrentDocList();
								CPM.ResetCurrentDocument();
								NotifyCurrentOrderChanged();
								if(CPM.OrderHList != null && CPM.OrderHList.size() > 0) {
									CPM.SetTabVisibility(CommonPrereqModule.Tab.tabOrders, View.VISIBLE);
									NotifyTabContentChanged(CommonPrereqModule.Tab.tabOrders, R.id.orderPrereqOrderListView);
									GotoTab(CommonPrereqModule.Tab.tabOrders, R.id.orderPrereqOrderListView, -1, -1);
								}
								CPM.SetTabVisibility(CommonPrereqModule.Tab.tabCurrentOrder, View.GONE);
							}
							else {
								; // @todo
							}
						}
					}
				}
				break;
		}
		return result;
	}
}