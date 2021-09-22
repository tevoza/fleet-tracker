using Microsoft.EntityFrameworkCore.Migrations;

namespace FleetTracker.Migrations
{
    public partial class AndroidID : Migration
    {
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<string>(
                name: "AndroidID",
                table: "Trucker",
                type: "varchar(450)",
                maxLength: 450,
                nullable: true);
        }

        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropColumn(
                name: "AndroidID",
                table: "Trucker");
        }
    }
}
