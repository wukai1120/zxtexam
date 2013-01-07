package net.whzxt.zxtexam;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ActionManager {
	public static interface OnStatusChange {
		public abstract void onFault(List<Integer> indexs);

		public abstract void onStop();
	}

	public Boolean IsRunning = false;
	public int TotalPoints = 100; // 总分
	private Timer _timer;
	private int _step = 1;
	private int _timeout = 0;

	public void setTimeout(int t) {
		_timeout = t * (1000 / Metadata.PERIOD);
	}

	private List<BaseAction> _listActions;

	public void setActions(List<BaseAction> actions) {
		_listActions = actions;
	}

	public List<BaseAction> getActions() {
		return _listActions;
	}

	public BaseAction getAction(int index) {
		return _listActions.get(index);
	}

	private OnStatusChange _onStatusChange;

	public void setOnStatusChange(OnStatusChange osc) {
		_onStatusChange = osc;
	}

	public BaseAction GetActionObject(Metadata md, int dataid, int times, int max, int min) {
		if (dataid < 20) {
			return new SignalAction(md, dataid, times, min, max);
		}
		if (dataid < 31) {
			return new NumberAction(md, dataid, times, min, max);
		}
		if (dataid == 31) {
			return new AngleAction(md, dataid, times, min, max);
		}
		return null;
	}

	public void Start() {
		if (_timer == null) {
			IsRunning = true;
			_step = 1;
			_timer = new Timer();
			_timer.schedule(new TimerTask() {
				@Override
				public void run() {
					if (TotalPoints < 90) {
						Stop();
						return;
					}
					if (_timeout > 0) {
						_timeout--;
						if (_timeout == 0) {
							List<Integer> list = null;
							for (int i = 0; i < _listActions.size(); i++) {
								if (!_listActions.get(i).IsWaitTimeout()) {
									if (!_listActions.get(i).CheckOK(_step)) {
										if (list == null) {
											list = new ArrayList<Integer>();
										}
										list.add(i);
										TotalPoints = TotalPoints - _listActions.get(i).Fenshu;
										if (TotalPoints < 90) {
											break;
										}
									}
								}
							}
							if (list != null) {
								_onStatusChange.onFault(list);
							}
							Stop();
							return;
						}
						Boolean b = true;
						for (int i = 0; i < _listActions.size(); i++) {
							if (!_listActions.get(i).IsStepOK(_step)) {
								b = false;
							}
						}
						if (b) {
							_step++;
							b = false;
							for (int i = 0; i < _listActions.size(); i++) {
								if (_listActions.get(i).Step == _step) {
									b = true;
								}
							}
							if (!b) {
								_step--;
								for (int i = 0; i < _listActions.size(); i++) {
									if (_listActions.get(i).IsWaitTimeout()) {
										b = true;
									}
								}
							}
							if (!b) {
								Stop();
								return;
							}
						}
						List<Integer> list = null;
						for (int i = 0; i < _listActions.size(); i++) {
							_listActions.get(i).CheckOK(_step);
							if (_listActions.get(i).CheckError(_step)) {
								if (list == null) {
									list = new ArrayList<Integer>();
								}
								list.add(i);
								TotalPoints = TotalPoints - _listActions.get(i).Fenshu;
								if (TotalPoints < 90) {
									break;
								}
							}
						}
						if (list != null) {
							_onStatusChange.onFault(list);
						}
					}
				}
			}, 1000, Metadata.PERIOD);
		}
	}

	public void Stop() {
		if (_timer != null) {
			_timer.cancel();
			_timer = null;
		}
		IsRunning = false;
		_onStatusChange.onStop();
	}

	public void destroy() {
		if (_timer != null) {
			_timer.cancel();
			_timer = null;
		}
		IsRunning = false;
	}
}