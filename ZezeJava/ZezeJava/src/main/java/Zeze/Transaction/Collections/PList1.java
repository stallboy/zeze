package Zeze.Transaction.Collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.function.UnaryOperator;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Log;
import Zeze.Transaction.Transaction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.pcollections.Empty;

@SuppressWarnings("DataFlowIssue")
public class PList1<V> extends PList<V> {
	protected final @NotNull Meta1<V> meta;

	public PList1(@NotNull Class<V> valueClass) {
		meta = Meta1.getList1Meta(valueClass);
	}

	private PList1(@NotNull Meta1<V> meta) {
		this.meta = meta;
	}

	@Override
	public boolean add(@NotNull V item) {
		//noinspection ConstantValue
		if (item == null)
			throw new IllegalArgumentException("null item");

		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var listLog = (LogList1<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			return listLog.add(item);
		}
		list = list.plus(item);
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean remove(@NotNull Object item) {
		if (isManaged()) {
			var listLog = (LogList1<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			return listLog.remove((V)item);
		}
		var newList = list.minus(item);
		if (newList == list)
			return false;
		list = newList;
		return true;
	}

	@Override
	public void clear() {
		if (isEmpty())
			return;
		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var listLog = (LogList1<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			listLog.clear();
		} else
			list = Empty.vector();
	}

	@Override
	public @NotNull V set(int index, @NotNull V item) {
		//noinspection ConstantValue
		if (item == null)
			throw new IllegalArgumentException("null item");

		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var listLog = (LogList1<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			return listLog.set(index, item);
		}
		var old = list.get(index);
		list = list.with(index, item);
		return old;
	}

	@Override
	public void add(int index, @NotNull V item) {
		//noinspection ConstantValue
		if (item == null)
			throw new IllegalArgumentException("null item");

		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var listLog = (LogList1<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			listLog.add(index, item);
		} else
			list = list.plus(index, item);
	}

	@Override
	public @NotNull V remove(int index) {
		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var listLog = (LogList1<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			return listLog.remove(index);
		}
		V old = list.get(index);
		list = list.minus(index);
		return old;
	}

	@Override
	public boolean addAll(@NotNull Collection<? extends V> items) {
		if (items.isEmpty())
			return false;
		if (items instanceof PList1)
			items = ((PList1<? extends V>)items).getList(); // more stable
		for (V v : items) {
			if (v == null)
				throw new IllegalArgumentException("null item");
		}

		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var listLog = (LogList1<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			return listLog.addAll(items);
		}
		list = list.plusAll(items);
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean removeAll(@NotNull Collection<?> c) {
		if (c.isEmpty() || isEmpty())
			return false;
		if (isManaged()) {
			var listLog = (LogList1<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			return listLog.removeAll((Collection<V>)c);
		}
		var newList = list.minusAll(c);
		if (newList == list)
			return false;
		list = newList;
		return true;
	}

	@Override
	public void replaceAll(@NotNull UnaryOperator<V> operator) {
		if (isEmpty())
			return;
		var tmpList = new ArrayList<V>(size());
		for (V v : this) {
			v = operator.apply(v);
			if (v == null)
				throw new IllegalStateException("null item");
			tmpList.add(v);
		}
		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var listLog = (LogList1<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			listLog.clear();
			listLog.addAll(tmpList);
		} else
			list = Empty.<V>vector().plusAll(tmpList);
	}

	@Override
	public void sort(@Nullable Comparator<? super V> c) {
		if (isEmpty())
			return;
		var tmpList = new ArrayList<>(this);
		tmpList.sort(c);
		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var listLog = (LogList1<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			listLog.clear();
			listLog.addAll(tmpList);
		} else
			list = Empty.<V>vector().plusAll(tmpList);
	}

	@Override
	public @NotNull LogBean createLogBean() {
		return new LogList1<>(parent(), variableId(), this, list, meta);
	}

	@Override
	public void followerApply(@NotNull Log _log) {
		@SuppressWarnings("unchecked")
		var log = (LogList1<V>)_log;
		var tmp = list;
		for (var opLog : log.getOpLogs()) {
			switch (opLog.op) {
			case LogList1.OpLog.OP_MODIFY:
				tmp = tmp.with(opLog.index, opLog.value);
				break;
			case LogList1.OpLog.OP_ADD:
				tmp = tmp.plus(opLog.index, opLog.value);
				break;
			case LogList1.OpLog.OP_REMOVE:
				tmp = tmp.minus(opLog.index);
				break;
			case LogList1.OpLog.OP_CLEAR:
				tmp = Empty.vector();
				break;
			}
		}
		list = tmp;
	}

	public void assign(@NotNull PList1<V> plist) {
		var items = plist.getList();
		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var listLog = (LogList1<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			listLog.clear();
			listLog.addAll(items);
		} else
			list = items;
	}

	@Override
	public @NotNull PList1<V> copy() {
		var copy = new PList1<>(meta);
		copy.list = getList();
		return copy;
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		var tmp = getList();
		bb.WriteUInt(tmp.size());
		var encoder = meta.valueEncoder;
		for (var e : tmp)
			encoder.accept(bb, e);
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		clear();
		var decoder = meta.valueDecoder;
		for (int i = bb.ReadUInt(); i > 0; i--)
			add(decoder.apply(bb));
	}
}
