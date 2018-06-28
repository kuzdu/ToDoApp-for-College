package rothkegel.com.todoapp.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.jetbrains.anko.toast
import rothkegel.com.todoapp.api.connector.ToDoServiceClient
import rothkegel.com.todoapp.api.connector.utils.ToDo
import rothkegel.com.todoapp.api.connector.utils.User
import rothkegel.com.todoapp.database.ToDoDBHelper
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket


const val baseUrl = "http://192.168.178.20"
const val baseUrlIp = "192.168.178.20"
const val port = 8080


open class ToDoAbstractActivity : AppCompatActivity() {


    internal val toDoDetailUpdateRequestCode = 10
    internal val toDoDetailAddRequestCode = 11
    internal val toDoIdentifier = "toDoIdentifier"
    internal val removedToDoIdentifier = "removedToDoIdentifier"

    lateinit var toDoDBHelper: ToDoDBHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        toDoDBHelper = ToDoDBHelper(this)





        /*
        val todo = DatabaseToDo()
        todo.name = "Name"
        todo.description = "This is a description"
        todo.dueDate = "Date"
        todo.favorite = true
        todo.done = true
        toDoDBHelper.insertToDo(todo)


        val todos = toDoDBHelper.readAllTodos()
        print(todos.first().toString())

        for (todo in todos) {
            Log.d("OUTPUT", todo.toString())
        }*/
        /*

    //        val timestamp = DateTool.convertUnixtimeToDate("27/06/2013 13:31:00")
    //        val timeString = DateTool.getDateTime(timestamp)
    //        toast(timeString)

        override fun onToDosFetched(toDos: Array<ToDo>?) {
            super.onToDosFetched(toDos)

        }*/

        /* override fun onToDoAdded(toDo: ToDo?) {
             super.onToDoAdded(toDo)

             val updatedToDo = ToDo()
             val updatedLocation = Location("New Home", LatLng(8.837, 50.18))
             val updatedContacts = arrayOf("Frank", "Andy", "Tim")

             updatedToDo.id = 1234
             updatedToDo.name = "Testname"
             updatedToDo.description = "New Updated Description"
             updatedToDo.done = false
             updatedToDo.expiry = 1
             updatedToDo.favourite = false
             updatedToDo.contacts = updatedContacts
             updatedToDo.location = updatedLocation
             updateToDo(updatedToDo, 1234)
         }*/
    }

    //"adapter"

    open fun insertToDoSQL(toDo: ToDo) {



//        toDoDBHelper.insertToDo()
    }




    //listener
    open fun onToDoUpdated(toDo: ToDo?) {
        toast("Updated ${toDo?.name}")
    }

    open fun onToDosFetched(todos: Array<ToDo>?) {
        // toast("Got ${todos?.size} ToDos")
    }

    open fun onLoggedInUser(loggedIn: Boolean?) {
        // toast("Logged in $loggedIn")
    }

    fun onSingleTodoFetched(toDo: ToDo?) {
        // toast("ToDo Name ${toDo?.name.toString()}")
    }

    fun onError(error: Throwable) {
        toast(error.localizedMessage)
//        error.printStackTrace()
    }

    fun onDeletedAllToDos(removed: Boolean?) {
        // toast("Deleted: $removed")
    }

    open fun onToDoRemoved(removed: Boolean?) {
        //toast("Removed: $removed")
    }

    open fun onToDoAdded(toDo: ToDo?) {
        toast("Added ${toDo?.name}")
    }


    //request
    open fun fetchToDos() {
        ToDoServiceClient.fetchToDos().observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe({ result ->
                    onToDosFetched(result.body())
                }, { error ->
                    onError(error)
                })
    }

    fun fetchToDo(id: Int) {
        ToDoServiceClient.fetchToDo(id).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe({ result ->
                    onSingleTodoFetched(result.body())
                }, { error ->
                    onError(error)
                })
    }

    open fun loginUser(user: User) {
        ToDoServiceClient.loginUser(user).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe({ result ->
                    onLoggedInUser(result.body())
                }, { error ->
                    onError(error)
                })
    }

    fun deleteAllToDos() {
        ToDoServiceClient.removeAllToDos().observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe({ result ->
                    onDeletedAllToDos(result.body())
                }, { error ->
                    onError(error)
                })

    }

    fun removeToDo(id: Int) {
        ToDoServiceClient.removeToDo(id).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe({ result ->
                    onToDoRemoved(result.body())
                }, { error ->
                    onError(error)
                })
    }

    fun addToDo(toDo: ToDo) {
        ToDoServiceClient.addToDo(toDo).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe({ result ->
                    onToDoAdded(result.body())
                }, { error ->
                    onError(error)
                })
    }

    fun updateToDo(toDo: ToDo) {
        ToDoServiceClient.updateToDo(toDo, toDo.id).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe({ result ->
                    onToDoUpdated(result.body())
                }, { error ->
                    onError(error)
                })
    }

    fun hasInternetConnection(): Single<Boolean> {
        return Single.fromCallable {
            try {
                // Connect to Google DNS to check for connection
                val timeoutMs = 1500
                val socket = Socket()
                val socketAddress = InetSocketAddress(baseUrlIp, port)

                socket.connect(socketAddress, timeoutMs)
                socket.close()

                true
            } catch (e: IOException) {
                false
            }
        }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }
}
