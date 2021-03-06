package rothkegel.com.todoapp.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuItem
import com.google.gson.Gson
import kotlinx.android.synthetic.main.todo_list_activity.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.toast
import org.jetbrains.anko.yesButton
import rothkegel.com.todoapp.R
import rothkegel.com.todoapp.api.connector.utils.ToDo
import rothkegel.com.todoapp.tools.SortTool


class ToDoListActivity : ToDoAbstractActivity(), ClickListener {

    private var toDos: ArrayList<ToDo> = ArrayList()
    private var syncRequests = 0
    private var triesToRemoveAllToDos = 0


    private var doubleBackToExitPressedOnce = false

    override fun onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            if(Build.VERSION.SDK_INT in 16..20){
                finishAffinity()
            } else if(Build.VERSION.SDK_INT >= 21){
                finishAndRemoveTask()
            }

            System.exit(0)
            return
        }

        this.doubleBackToExitPressedOnce = true
        toast("Klicke noch einmal zurück, um dich auszuloggen. Die App startet danach neu.")

        Handler().postDelayed(Runnable { doubleBackToExitPressedOnce = false }, 2000)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.todo_list_activity)

        showNoInternetWarning()
        fetchToDos()
        setAddToDoClickListener()
    }

    //interface implements
    override fun onDoneClicked(toDo: ToDo) {
        updateToDoSQL(toDo)
    }

    override fun onFavouriteClicked(toDo: ToDo) {
        updateToDoSQL(toDo)
    }

    override fun onToDoItemClicked(position: Int) {
        goToToDoDetail(position)
    }

    private fun showNoInternetWarning() {
        if (hasInternet()) {
            return
        }

        alert("Du hast keine Verbindung zur API. D.h. deine ToDos werden nur lokal auf deinem Gerät gespeichert.\n\n¯\\_(ツ)_/¯") {
            title = "Keine Verbidung zur API"
            yesButton { }
        }.show()
    }

    // CLICK LISTENER
    private fun setAddToDoClickListener() {
        addToDoActionButton.setOnClickListener {
            goToToDoDetail()
        }
    }

    //SYNCING METHODS
    private fun syncLocalToDos(isRemoved: Boolean?) {
        triesToRemoveAllToDos += 1
        if (isRemoved != null && isRemoved) {
            //toast("Alle Web-ToDos gelöscht - starte Synchronisation")
            sendLocalToDosToWeb()
            return
        }

        //fallback
        if (triesToRemoveAllToDos > 2) {
            toast("3 Mal versucht alle ToDos zu löschen. Es hat nicht funktioniert.")
            return
        }
        removeAllAPIToDos()
    }

    private fun sendLocalToDosToWeb() {
        val localToDos = getToDosSQL()
        if (localToDos.isEmpty()) {
            return
        }
        syncRequests = localToDos.size

        for (localToDo in localToDos) {
            addToDo(localToDo)
        }
    }


    // OPTIONS MENU
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.sort_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle presses on the action bar menu items
        when (item.itemId) {
            R.id.menu_done_date_favourite -> {
                this.toDos = SortTool.getSortedByDoneThenExpiryThenFavourite(this.toDos)
                setAdapter(toDos.toList())
                return true
            }
            R.id.menu_done_favourite_date -> {
                this.toDos = SortTool.getSortedByDoneThenFavouriteThenExpiry(this.toDos)
                setAdapter(toDos.toList())
                return true
            }
            R.id.menu_non_done_date_favourite -> {
                this.toDos = SortTool.getSortedByNonDoneThenExpiryThenFavourite(this.toDos)
                setAdapter(toDos.toList())
                return true
            }
            R.id.menu_non_done_favourite_date -> {
                this.toDos = SortTool.getSortedByNonDoneThenFavouriteThenExpiry(this.toDos)
                setAdapter(toDos.toList())
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    // GO TO NEXT DETAIL
    private fun goToToDoDetail() {
        val i = Intent(this, ToDoDetailActivity::class.java)
        startActivityForResult(i, toDoDetailAddRequestCode)
    }

    private fun goToToDoDetail(position: Int) {
        val i = Intent(this, ToDoDetailActivity::class.java)
        val toDo = toDos[position]
        val gson = Gson()
        i.putExtra(toDoIdentifier, gson.toJson(toDo))
        startActivityForResult(i, toDoDetailUpdateRequestCode)
    }

    //ON RESPONSE - can called by sql and api
    override fun onToDoUpdated(toDo: ToDo?) {
        super.onToDoUpdated(toDo)
        if (toDo == null) return
        updateLocalToDosWith(toDo)
        //toast(getString(R.string.to_do_list_successful_update_message))
    }


    override fun onRemoveAllAPIToDos(isRemoved: Boolean?) {
        super.onRemoveAllAPIToDos(isRemoved)
        syncLocalToDos(isRemoved)
    }

    override fun onToDosFetched(toDos: Array<ToDo>?) {
        super.onToDosFetched(toDos)

        if (hasInternet()) {
            val localToDos = getToDosSQL()
            if (localToDos.isNotEmpty()) {
                removeAllAPIToDos()
                return
            }

            //sync web api calls to local db
            clearDataBaseEntries()
            if (toDos == null) {
                return
            }
            for (toDo in toDos) {
                insertToDoSQL(toDo)
            }
        }

        //show todos
        if (toDos == null) return
        this.toDos = ArrayList(toDos.toList())
        this.toDos = SortTool.getSortedByNonDoneThenFavouriteThenExpiry(this.toDos)
        todo_list_items.layoutManager = LinearLayoutManager(this)
        setAdapter(this.toDos.toList())
    }

    override fun onToDoAdded(toDo: ToDo?) {
        super.onToDoAdded(toDo)

        if (toDo == null) {
            toast(getString(R.string.to_do_list_empty_to_do_error_message))
            return
        }

        val successFul = toDoDBHelper.deleteById(toDo.id.toLong())
        if (!successFul) {
            toast(getString(R.string.to_do_list_to_do_not_find_in_db_error_message) + toDo.id)
            return
        }

        syncRequests -= 1

        if (syncRequests <= 0) {
            fetchToDos()
        //    toast("Alle lokalen ToDos wurde in die API eingepflegt.")
        }
    }


    // RECYCLER VIEW
    private fun setAdapter(toDos: List<ToDo>) {
        val adapter = ToDoListAdapter(ArrayList(toDos.toList()), this)
        todo_list_items.adapter = adapter
        adapter.setClickListenerCallback(this)
        todo_list_items.adapter.notifyDataSetChanged()
    }

    // REFRESH OPERATIONS FOR RECYCLER VIEW
    private fun updateLocalToDosWith(toDo: ToDo) {
        val foundToDo = toDos.find { t -> t.id == toDo.id } ?: return

        val index = toDos.indexOf(foundToDo)
        this.toDos[index] = toDo

        setAdapter(this.toDos.toList())
    }

    private fun removeLocalToDoWith(id: Int) {
        val foundToDo = toDos.find { t -> t.id == id } ?: return

        val index = toDos.indexOf(foundToDo)
        toDos.removeAt(index)

        setAdapter(this.toDos.toList())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)


        if (requestCode == toDoDetailAddRequestCode) {
            val toDoAsString = data?.getStringExtra(toDoIdentifier)

            //update
            if (!toDoAsString.isNullOrEmpty()) {
                val gson = Gson()
                val parsedToDo = gson.fromJson<ToDo>(toDoAsString, ToDo::class.java)

                if (parsedToDo != null) {
                    toDos.add(parsedToDo)
                    setAdapter(this.toDos.toList())
                }
                return
            }
        }
        if (requestCode == toDoDetailUpdateRequestCode) {
            val toDoAsString = data?.getStringExtra(toDoIdentifier)

            //update
            if (!toDoAsString.isNullOrEmpty()) {
                val gson = Gson()
                val parsedToDo = gson.fromJson<ToDo>(toDoAsString, ToDo::class.java)

                if (parsedToDo != null) {
                    updateLocalToDosWith(parsedToDo)
                }
                return
            }

            //delete
            val toDoId = data?.getIntExtra(removedToDoIdentifier, -1)

            if (toDoId == null || toDoId == -1) {
                return
            }
            removeLocalToDoWith(toDoId)
        }
    }
}