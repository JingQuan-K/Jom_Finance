package com.example.jom_finance.databinding

import android.graphics.Color.WHITE
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.jom_finance.R
import com.example.jom_finance.models.Account
import com.maltaisn.icondialog.pack.IconDrawableLoader
import com.maltaisn.icondialog.pack.IconPackLoader
import com.maltaisn.iconpack.defaultpack.createDefaultIconPack

class AccountListAdapter(private val accountList : ArrayList<Account>, private val listener : OnItemClickListener) : RecyclerView.Adapter<AccountListAdapter.ListViewHolder>(){
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_account,parent,false)
        return ListViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
        val account : Account = accountList[position]

        holder.name.text = account.accountName.toString()

        holder.amount.text = "RM " + String.format("%.2f", account.accountAmount)

        //Icon
        val loader = IconPackLoader(holder.icon.context)
        val iconPack = createDefaultIconPack(loader)
        val drawable = iconPack.getIconDrawable(account.accountIcon!!, IconDrawableLoader(holder.icon.context))
        holder.icon.setImageDrawable(drawable)
        //Change icon to white color
        holder.icon.setColorFilter(WHITE)

        //Color of account
        holder.iconBg.setColorFilter(account.accountColor!!)

    }

    override fun getItemCount(): Int {
        return accountList.size
    }

    inner class ListViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView), View.OnClickListener{
        val name : TextView = itemView.findViewById(R.id.accountNameList_text)
        val amount : TextView = itemView.findViewById(R.id.accountAmountList_text)
        val icon : ImageView = itemView.findViewById(R.id.accountIconList_img)
        val iconBg : ImageView = itemView.findViewById(R.id.accountIconBgList_img)

        init{
            itemView.setOnClickListener(this)
        }
        override fun onClick(v: View?) {
            if(this.adapterPosition != RecyclerView.NO_POSITION){
                listener.onItemClick(this.adapterPosition)
            }
        }
    }

    interface OnItemClickListener{
        fun onItemClick(position : Int)
    }
}