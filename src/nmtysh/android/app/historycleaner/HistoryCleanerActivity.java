/*
BSD License

Copyright(c) 2011, N.Matayoshi All rights reserved.

Redistribution and use in source and binary forms, with or without modification, 
are permitted provided that the following conditions are met:

・Redistributions of source code must retain the above copyright notice, 
  this list of conditions and the following disclaimer.
・Redistributions in binary form must reproduce the above copyright notice, 
  this list of conditions and the following disclaimer in the documentation 
  and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
ARE DISCLAIMED. 
IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, 
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF 
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE 
OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package nmtysh.android.app.historycleaner;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.Browser;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/*
 * TODO: favicon の表示
 * TODO: Title がない履歴の対処(Title == URL な場合に Title を省略する)
 * TODO: スクロールしやすくする
 */
/**
 * メイン Activity
 * 
 * @author n.matayoshi
 */
public class HistoryCleanerActivity extends Activity {
	private CustomCursorAdapter adapter = null;
	private ListView listView = null;
	private Button deleteButton = null;
	private TextView checkedItems = null;
	private TextView allItems = null;
	private Cursor cursor = null;
	private int checkedCount = 0;

	private int sortOrder = 3;
	private final String SORT_ORDER = "Sort_Order";
	private final String[] SORT_PARAM = { Browser.BookmarkColumns.TITLE,
			Browser.BookmarkColumns.TITLE + " DESC",
			Browser.BookmarkColumns.URL, Browser.BookmarkColumns.URL + " DESC",
			Browser.BookmarkColumns.DATE,
			Browser.BookmarkColumns.DATE + " DESC",
			Browser.BookmarkColumns.VISITS,
			Browser.BookmarkColumns.VISITS + " DESC", };

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// リストのクリックイベントを捕捉する
		listView = (ListView) findViewById(android.R.id.list);
		listView.setOnItemClickListener(itemClickListener);
		listView.setOnItemLongClickListener(itemLongClickListener);

		// データベースとリストビューを関連付ける
		String[] from = new String[] { Browser.BookmarkColumns.TITLE,
				Browser.BookmarkColumns.URL, Browser.BookmarkColumns.VISITS, };
		int[] to = new int[] { R.id.title, R.id.url, R.id.view_count, };
		adapter = new CustomCursorAdapter(this, null, from, to);
		listView.setAdapter(adapter);

		// ボタンのクリックイベントを捕捉する
		deleteButton = (Button) findViewById(R.id.del_button);
		deleteButton.setOnClickListener(clickListener);

		checkedItems = (TextView) findViewById(R.id.checked_items);
		allItems = (TextView) findViewById(R.id.all_items);

		// 設定の読み込み
		load_option();
	}

	/**
	 * Activity が可視状態になった段階で履歴の一覧を取得します。
	 */
	@Override
	protected void onStart() {
		super.onStart();

		getHistory();

		checkedCount = 0;
		dispCheckedCount();
	}

	/**
	 * オプションメニューを作成します。
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_view_menu, menu);
		return result;
	}

	/**
	 * オプションメニューのクリックイベントを処理します。
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.main_menu_all_check: // すべて選択
			allCheck();
			break;
		case R.id.main_menu_all_uncheck: // すべて選択解除
			allUnCheck();
			break;
		case R.id.main_menu_sort: // 並べ替え
			showSortDialog();
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	// リストアイテムのクリックイベントリスナー
	OnItemClickListener itemClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			// 選択状態を切り替える
			CheckBox checkBox = (CheckBox) view.findViewById(R.id.del_check);
			checkBox.toggle();

			// 選択件数を更新
			if (checkBox.isChecked()) {
				checkedCount++;
			} else {
				checkedCount--;
			}
			dispCheckedCount();
		}
	};

	// ボタンのイベントリスナー
	OnClickListener clickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			int tindex = cursor.getColumnIndex(Browser.BookmarkColumns.TITLE);

			// チェック済みリストの取得
			SparseBooleanArray array = listView.getCheckedItemPositions();
			List<String> list = new LinkedList<String>();
			for (int i = 0; i < array.size(); i++) {
				if (array.valueAt(i)) { // チェックされていないものも混じっている
					cursor.moveToPosition(array.keyAt(i));
					list.add(cursor.getString(tindex));
				}
			}
			// チェック済みのものが無かった。
			if (list.size() == 0) {
				Toast.makeText(HistoryCleanerActivity.this,
						R.string.not_checked, Toast.LENGTH_SHORT).show();
				return;
			}

			AlertDialog.Builder dialog = new AlertDialog.Builder(
					HistoryCleanerActivity.this);
			dialog.setTitle("以下の" + list.size() + "件を削除しますか?");
			dialog.setItems((String[]) list.toArray(new String[0]), null);
			dialog.setPositiveButton(R.string.delete,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							delete(); // 履歴の削除
						}
					});
			dialog.setNegativeButton(R.string.cancel,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});
			dialog.create().show();
		}
	};

	// リストアイテムのロングクリックイベントのリスナー
	OnItemLongClickListener itemLongClickListener = new OnItemLongClickListener() {
		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view,
				int position, long id) {
			// 現在の選択状態を保持する
			SparseBooleanArray array = listView.getCheckedItemPositions();
			Set<Integer> set = new LinkedHashSet<Integer>();
			for (int i = 0; i < array.size(); i++) {
				if (array.valueAt(i)) {
					int key = array.keyAt(i);
					key = key > position ? key - 1 : key;
					set.add(key);
				}
			}

			// 選択した履歴を削除する
			int uindex = cursor.getColumnIndex(Browser.BookmarkColumns.URL);
			cursor.moveToPosition(position);
			Browser.deleteFromHistory(getContentResolver(),
					cursor.getString(uindex));

			allUnCheck();
			// 再取得
			getHistory();

			// リストの選択状態を反映させる
			// TODO: 元の順序と再取得後の順序が異なっている可能性も考慮する
			for (Iterator<Integer> it = set.iterator(); it.hasNext();) {
				int index = it.next();
				listView.setItemChecked(index, true);
			}

			// 選択件数を更新
			checkedCount = set.size();
			dispCheckedCount();

			return true;
		}
	};

	/**
	 * 履歴を取得します。
	 */
	private void getHistory() {
		// 古いカーソルがある場合は明示的に閉じる。
		if (cursor != null) {
			stopManagingCursor(cursor);
			cursor.close();
			cursor = null;
		}

		// 取得するDBのカラム指定
		String[] projection = new String[] { Browser.BookmarkColumns.TITLE,
				Browser.BookmarkColumns.URL, Browser.BookmarkColumns.VISITS, };
		// ContentProviderから履歴情報を取得(ブックマーク登録されたものを除外する)
		cursor = getContentResolver().query(Browser.BOOKMARKS_URI, // URI
				projection, // カラム
				Browser.BookmarkColumns.BOOKMARK + " = ?", // selection
				new String[] { "0" }, // selectionArgs
				SORT_PARAM[sortOrder] // sortOrder
				);
		adapter.changeCursor(cursor);
		// ベースクラスにCursorのライフサイクルを管理させる
		startManagingCursor(cursor);

		// 全件数を表示
		allItems.setText(getString(R.string.all_items_label, cursor.getCount()));
	}

	/**
	 * すべての項目を選択解除します。
	 */
	private void allUnCheck() {
		int num = listView.getCount();
		for (int i = 0; i < num; i++) {
			listView.setItemChecked(i, false);
		}
		checkedCount = 0;
		dispCheckedCount();
	}

	/**
	 * すべての項目を選択します。
	 */
	private void allCheck() {
		int num = listView.getCount();
		for (int i = 0; i < num; i++) {
			listView.setItemChecked(i, true);
		}
		checkedCount = num;
		dispCheckedCount();
	}

	/**
	 * 履歴を削除します。
	 */
	private void delete() {
		int uindex = cursor.getColumnIndex(Browser.BookmarkColumns.URL);
		SparseBooleanArray array = listView.getCheckedItemPositions();

		for (int i = 0; i < array.size(); i++) {
			if (array.valueAt(i)) {
				cursor.moveToPosition(array.keyAt(i));
				Browser.deleteFromHistory(getContentResolver(),
						cursor.getString(uindex));
			}
		}
		allUnCheck();
		// 再取得
		getHistory();
	}

	/**
	 * 選択件数を表示します。
	 */
	private void dispCheckedCount() {
		checkedItems.setText(getString(R.string.checked_items_label, checkedCount));

		// 選択済みの件数が 0 件なら削除ボタンを無効化
		if (checkedCount == 0) {
			deleteButton.setEnabled(false);
		} else if (!deleteButton.isEnabled()) {
			deleteButton.setEnabled(true);
		}
	}

	/**
	 * ソートオプションダイアログを表示します。
	 */
	private void showSortDialog() {
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setTitle(R.string.sort_label);
		dialog.setSingleChoiceItems(R.array.sort, sortOrder,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						sortOrder = which;
						getHistory();
						// 設定の保存
						save_option();
					}
				});
		dialog.create().show();
	}

	/**
	 * 設定を読み込みます。
	 */
	private void load_option() {
		SharedPreferences preferences = getPreferences(MODE_PRIVATE);
		int tmp = preferences.getInt(SORT_ORDER, sortOrder);
		if (0 <= tmp && tmp < SORT_PARAM.length) {
			sortOrder = tmp;
		}
	}

	/**
	 * 設定を保存します。
	 */
	private void save_option() {
		SharedPreferences preferences = getPreferences(MODE_PRIVATE);
		Editor editor = preferences.edit();
		editor.putInt(SORT_ORDER, sortOrder);
		// 設定の保存
		editor.commit();
	}
}
// EOF