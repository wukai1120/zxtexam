package net.whzxt.zxtexam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;


public class ItemManager {
	public static interface OnStatusChange {
		public abstract void onFault(Action action);

		public abstract void onStop();
	}

	private Timer _timer;
	private final int Period = 100;
	public ArrayList<Action> _listActions;
	public Boolean _pause = true;
	private int _timeout;	
	private OnStatusChange _onStatusChange;
	private HashMap<Integer, Integer> _hashdata;
	private Metadata _md;
	private int _startAngle; // 开始时的GPS角度
	private int _step;
	private Boolean _stepfinish;

	public ItemManager(OnStatusChange osc, Metadata md) {
		_md = md;
		_onStatusChange = osc;
		_hashdata = new HashMap<Integer, Integer>();
		_pause = true;
		_timer = new Timer();
		_timer.schedule(new TimerTask() {
			@Override
			public void run() {
				if (!_pause && _timeout > 0) {
					_timeout--;
					if (_timeout == 0) {
						// 扣分
						for (int i = 0; i < _listActions.size(); i++) {
							if (_listActions.get(i).Step == _step && !_listActions.get(i).IsOK) {
								_onStatusChange.onFault(_listActions.get(i));
							}
						}
						// 结束
						Stop();
						return;
					} else {
						_stepfinish = true;
						for (int i = 0; i < _listActions.size(); i++) {
							if (_listActions.get(i).Step == _step && !_listActions.get(i).IsOK) {
								if (_listActions.get(i).Dataid < 20) {
									_stepfinish = false;
									break;
								} else if (_listActions.get(i).Dataid == 31) {
									if (_listActions.get(i).Min > 0) {
										_stepfinish = false;
									}
								} else {
									if (_listActions.get(i).Min > 0 && _listActions.get(i).Max == 0){
										_stepfinish = false;
									}
								}
							}
						}
						if (_stepfinish) {
							_step++;
							Boolean hasStep = false;
							for (int i = 0; i < _listActions.size(); i++) {
								if (_listActions.get(i).Step == _step) {
									hasStep = true;
									break;
								}
							}
							if (!hasStep) {
								Stop();
								return;
							}
						}
						for (int i = 0; i < _listActions.size(); i++) {
							if (_listActions.get(i).Step == _step && !_listActions.get(i).IsOK) {
								if (_listActions.get(i).Dataid < 20) {
									switch (_listActions.get(i).Times) {
									case 1:
										if (_md.getData(_listActions.get(i).Dataid) == 1) {
											_onStatusChange.onFault(_listActions.get(i));
											if (_listActions.get(i).Fenshu > 10) {// 扣分超过10分,结束
												Stop();
											}
										}
										break;
									case -1:
										if (_md.getData(_listActions.get(i).Dataid) == 0) {
											_onStatusChange.onFault(_listActions.get(i));
											if (_listActions.get(i).Fenshu > 10) {// 扣分超过10分,结束
												Stop();
											}
										}
										break;
									case 2:
										if (_md.getData(_listActions.get(i).Dataid) == 1) {
											_listActions.get(i).IsOK = true;
										}
										break;
									case -2:
										if (_md.getData(_listActions.get(i).Dataid) == 0) {
											_listActions.get(i).IsOK = true;
										}
										break;
									case 3:
										if (_hashdata.get(_listActions.get(i).Dataid) == null) {
											_hashdata.put(_listActions.get(i).Dataid, _md.getData(_listActions.get(i).Dataid));
										} else {
											_hashdata.put(_listActions.get(i).Dataid, _hashdata.get(_listActions.get(i).Dataid) + _md.getData(_listActions.get(i).Dataid));
										}
										if (_hashdata.get(_listActions.get(i).Dataid) >= 1 * (1000 / Period)) {
											_listActions.get(i).IsOK = true;
										}
										break;
									case -3:
										if (_hashdata.get(_listActions.get(i).Dataid) == null) {
											_hashdata.put(_listActions.get(i).Dataid, _md.getData(_listActions.get(i).Dataid) == 1 ? 0 : 1);
										} else {
											_hashdata.put(_listActions.get(i).Dataid, _hashdata.get(_listActions.get(i).Dataid) + (_md.getData(_listActions.get(i).Dataid) == 1 ? 0 : 1));
										}
										if (_hashdata.get(_listActions.get(i).Dataid) >= 1 * (1000 / Period)) {
											_listActions.get(i).IsOK = true;
										}
										break;
									case 4:
										if (_hashdata.get(_listActions.get(i).Dataid) == null) {
											_hashdata.put(_listActions.get(i).Dataid, _md.getData(_listActions.get(i).Dataid));
										} else {
											_hashdata.put(_listActions.get(i).Dataid, _hashdata.get(_listActions.get(i).Dataid) + _md.getData(_listActions.get(i).Dataid));
										}
										if (_hashdata.get(_listActions.get(i).Dataid) >= 3 * (1000 / Period)) {
											_listActions.get(i).IsOK = true;
										}
										break;
									case -4:
										if (_hashdata.get(_listActions.get(i).Dataid) == null) {
											_hashdata.put(_listActions.get(i).Dataid, _md.getData(_listActions.get(i).Dataid) == 1 ? 0 : 1);
										} else {
											_hashdata.put(_listActions.get(i).Dataid, _hashdata.get(_listActions.get(i).Dataid) + (_md.getData(_listActions.get(i).Dataid) == 1 ? 0 : 1));
										}
										if (_hashdata.get(_listActions.get(i).Dataid) >= 3 * (1000 / Period)) {
											_listActions.get(i).IsOK = true;
										}
										break;
									default:
										break;
									}
								} else if (_listActions.get(i).Dataid == 31) {
									if (_startAngle == -1) {
										_startAngle = _md.getData(31);
									} else {
										if (_listActions.get(i).Max > 0) {
											if (_md.getData(31) - _startAngle > _listActions.get(i).Max) {
												_onStatusChange.onFault(_listActions.get(i));
												if (_listActions.get(i).Fenshu > 10) {// 扣分超过10分,结束
													Stop();
													break;
												}
											}
										} else if (_listActions.get(i).Min > 0) {
											if (_md.getData(31) - _startAngle > _listActions.get(i).Min) {
												_listActions.get(i).IsOK = true;
											}
										}
									}
								} else {
									if (_listActions.get(i).Max > 0 && _listActions.get(i).Min > 0) {
										if (_md.getData(_listActions.get(i).Dataid) > _listActions.get(i).Max || _md.getData(_listActions.get(i).Dataid) < _listActions.get(i).Min) {
											_onStatusChange.onFault(_listActions.get(i));
											if (_listActions.get(i).Fenshu > 10) {// 扣分超过10分,结束
												Stop();
												break;
											}
										}
									} else {
										if (_listActions.get(i).Max > 0) {
											if (_md.getData(_listActions.get(i).Dataid) > _listActions.get(i).Max) {
												_onStatusChange.onFault(_listActions.get(i));
												if (_listActions.get(i).Fenshu > 10) {// 扣分超过10分,结束
													Stop();
													break;
												}
											}
										} else if (_listActions.get(i).Min > 0) {
											if (_md.getData(_listActions.get(i).Dataid) > _listActions.get(i).Min) {
												_listActions.get(i).IsOK = true;
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}, 1000, Period);
	}

	public void Start() {
		_step = 1;
		_startAngle = -1;
		_hashdata.clear();
		_pause = false;
	}

	public void Stop() {
		_pause = true;
		_onStatusChange.onStop();
	}
	
	public void Destroy(){ 
		_timer.cancel();
		_timer = null;
	}

	public void setTimeout(int timeout) {
		_timeout = timeout * (1000 / Period);
	}
}