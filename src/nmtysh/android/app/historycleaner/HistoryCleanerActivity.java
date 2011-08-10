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

import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Toast;

/**
 * メイン Activity
 * 
 * @author n.matayoshi
 */
public class HistoryCleanerActivity extends Activity {
	private CustomCursorAdapter adapter = null;
	ListView listView = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// リストのクリックイベントを捕捉する
		listView = (ListView) findViewById(android.R.id.list);
		listView.setOnItemClickListener(itemClickListener);

		// データベースとリストビューを関連付ける
		String[] from = new String[] { Browser.BookmarkColumns.TITLE,
				Browser.BookmarkColumns.URL, Browser.BookmarkColumns.VISITS, };
		int[] to = new int[] { R.id.title, R.id.url, R.id.view_count, };
		adapter = new CustomCursorAdapter(this, null, from, to);
		listView.setAdapter(adapter);

		// ボタンのクリックイベントを捕捉する
		Button button = (Button) findViewById(R.id.del_button);
		button.setOnClickListener(clickListener);
	}

	/**
	 * Activity が可視状態になった段階で履歴の一覧を取得します。
	 */
	@Override
	protected void onStart() {
		super.onStart();

		// 古いカーソルがある場合は明示的に閉じる。
		Cursor old = adapter.getCursor();
		if (old != null) {
			stopManagingCursor(old);
			old.close();
			old = null;
		}

		// 取得するDBのカラム指定
		String[] projection = new String[] { Browser.BookmarkColumns.TITLE,
				Browser.BookmarkColumns.URL, Browser.BookmarkColumns.VISITS, };
		// ContentProviderから履歴情報を取得(ブックマーク登録されたものを除外する)
		Cursor c = getContentResolver().query(Browser.BOOKMARKS_URI, // URI
				projection, // カラム
				Browser.BookmarkColumns.BOOKMARK + " = ?", // selection
				new String[] { "0" }, // selectionArgs
				Browser.BookmarkColumns.DATE // sortOrder
				);
		// listView.invalidateViews();
		adapter.changeCursor(c);
		// adapter.notifyDataSetChanged();
		// ベースクラスにCursorのライフサイクルを管理させる
		startManagingCursor(c);
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
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	// リストのイベントリスナー
	OnItemClickListener itemClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			// 選択状態を切り替える
			((CheckBox) view.findViewById(R.id.del_check)).toggle();
		}
	};

	// ボタンのイベントリスナー
	OnClickListener clickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			Cursor c = adapter.getCursor();
			int tindex = c.getColumnIndex(Browser.BookmarkColumns.TITLE);

			// チェック済みリストの取得
			SparseBooleanArray array = listView.getCheckedItemPositions();
			List<String> list = new LinkedList<String>();
			for (int i = 0; i < array.size(); i++) {
				if (array.valueAt(i)) { // チェックされていないものも混じっている
					c.moveToPosition(array.keyAt(i));
					list.add(c.getString(tindex));
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

	/**
	 * すべて選択解除
	 */
	private void allUnCheck() {
		int num = listView.getCount();
		for (int i = 0; i < num; i++) {
			listView.setItemChecked(i, false);
		}
	}

	/**
	 * すべて選択
	 */
	private void allCheck() {
		int num = listView.getCount();
		for (int i = 0; i < num; i++) {
			listView.setItemChecked(i, true);
		}
	}

	/**
	 * 履歴の削除
	 */
	private void delete() {
		Cursor c = adapter.getCursor();
		int uindex = c.getColumnIndex(Browser.BookmarkColumns.URL);
		SparseBooleanArray array = listView.getCheckedItemPositions();

		for (int i = 0; i < array.size(); i++) {
			if (array.valueAt(i)) {
				c.moveToPosition(array.keyAt(i));
				Browser.deleteFromHistory(getContentResolver(),
						c.getString(uindex));
			}
		}
		// 選択状態をリセット
		allUnCheck();
	}
}
// EOF